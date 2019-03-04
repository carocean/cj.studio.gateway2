package cj.studio.gateway.stub;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.socket.util.SocketContants;
import cj.studio.gateway.stub.annotation.CjStubInContentKey;
import cj.studio.gateway.stub.annotation.CjStubInHead;
import cj.studio.gateway.stub.annotation.CjStubInParameter;
import cj.studio.gateway.stub.annotation.CjStubMethod;
import cj.studio.gateway.stub.annotation.CjStubReturn;
import cj.studio.gateway.stub.annotation.CjStubService;
import cj.studio.gateway.stub.util.StringTypeConverter;
import cj.ultimate.gson2.com.google.gson.Gson;
import cj.ultimate.gson2.com.google.gson.reflect.TypeToken;
import cj.ultimate.util.StringUtil;

public abstract class GatewayAppSiteRestStub implements IGatewayAppSiteWayWebView, StringTypeConverter {
	Map<String, Method> __stubMethods;

	public GatewayAppSiteRestStub() {
		CjService cj = this.getClass().getAnnotation(CjService.class);
		if (cj == null) {
			throw new EcmException("必须定义为服务");
		}
		loadStub(cj.name());
	}

	private void loadStub(String name) {
		this.__stubMethods = new HashMap<>();
		CjStubService found = null;
		Class<?> clazz = this.getClass();
		do {
			Class<?>[] faces = clazz.getInterfaces();
			for (Class<?> c : faces) {
				CjStubService an = c.getDeclaredAnnotation(CjStubService.class);
				if (an == null) {
					continue;
				}
				//
				found = an;
				Method[] methods = c.getDeclaredMethods();
				for (Method m : methods) {
					CjStubMethod sm = m.getDeclaredAnnotation(CjStubMethod.class);
					if (sm == null)
						continue;
					String mName = sm.alias();
					if (StringUtil.isEmpty(mName)) {
						mName = m.getName();
					}
					if (__stubMethods.containsKey(mName)) {
						throw new EcmException("RestStub不支持方法重载。冲突在：" + m);
					}
					__stubMethods.put(mName, m);
				}
			}
			clazz = clazz.getSuperclass();
		} while (clazz.equals(Object.class));
		if (found == null) {
			throw new EcmException("没有发现存根接口");
		}
		if (!name.startsWith(found.bindService()) && !found.bindService().startsWith(name)) {
			throw new EcmException("存根接口绑定服务名与宿主服务名不同");
		}
	}

	@Override
	public final void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		frame.content().accept(new MemoryContentReciever() {
			@Override
			public void done(byte[] b, int pos, int length) throws CircuitException {
				super.done(b, pos, length);
				String restCmd = frame.head(SocketContants.__frame_Head_Rest_Command);
				if (StringUtil.isEmpty(restCmd)) {
					restCmd = frame.parameter(SocketContants.__frame_Head_Rest_Command);
				}
				String stubClassName = frame.head(SocketContants.__frame_Head_Rest_Stub_Interface);
				if (StringUtil.isEmpty(stubClassName)) {
					stubClassName = frame.parameter(SocketContants.__frame_Head_Rest_Stub_Interface);
				}
				Class<?> clazz = GatewayAppSiteRestStub.this.getClass();
				try {
					Class<?> stub = Class.forName(stubClassName, true, clazz.getClassLoader());
					if (!stub.isAssignableFrom(clazz)) {
						throw new CircuitException("503", "当前webview未实现存根接口。" + stub + " 在 " + clazz);
					}
//					Method src = findMethod(restCmd, stub);
					Method src = __stubMethods.get(restCmd);
					if (src == null) {
						throw new CircuitException("404", "在存根接口中未找到方法：" + src);
					}
					Method dest = findDestMethod(clazz, src);
					if (dest == null) {
						throw new CircuitException("404", "在webview中未找到方法：" + dest);
					}
					Object[] args = getArgs(src, frame);
					doMethodBefore(dest,args,frame,circuit);
					Object ret = dest.invoke(GatewayAppSiteRestStub.this, args);
					if (ret != null) {
						CjStubReturn sr = dest.getDeclaredAnnotation(CjStubReturn.class);
						Class<?> retType = sr == null ? null : sr.type();
						if (retType == null) {
							retType = dest.getReturnType();
						}
						if (retType.equals(String.class)) {
							String str = (String) ret;
							circuit.content().writeBytes(str.getBytes());
						} else {
							circuit.content().writeBytes(new Gson().toJson(ret).getBytes());
						}
					}
					doMethodAfter(dest,args,frame,circuit);
				} catch (Exception e) {
					if (e instanceof CircuitException) {
						throw (CircuitException) e;
					}
					if (e instanceof InvocationTargetException) {
						InvocationTargetException inv = (InvocationTargetException) e;
						throw new CircuitException("503", inv.getTargetException());
					}
					throw new CircuitException("503", e);
				}
			}
		});

	}

	protected  void doMethodAfter(Method m, Object[] args, Frame frame, Circuit circuit)throws CircuitException {};

	protected  void doMethodBefore(Method m, Object[] args, Frame frame, Circuit circuit)throws CircuitException {};

	private Object[] getArgs(Method src, Frame frame) throws CircuitException {
		Map<String, Object> postContent = null;
		String cntText = "";
		if ("post".equalsIgnoreCase(frame.command())) {
			byte[] b = frame.content().readFully();
			cntText = new String(b);
			postContent = new Gson().fromJson(cntText, new TypeToken<HashMap<String, Object>>() {
			}.getType());
		}
		Parameter[] arr = src.getParameters();
		Object[] args = new Object[arr.length];
		for (int i = 0; i < arr.length; i++) {
			Parameter p = arr[i];
			CjStubInHead sih = p.getAnnotation(CjStubInHead.class);
			if (sih != null) {
				String value = frame.head(sih.key());
				try {
					if (!StringUtil.isEmpty(value)) {
						value = URLDecoder.decode(value, "utf-8");
					}
				} catch (UnsupportedEncodingException e) {
				}
				Class<?> pType =  sih.type();
				Class<?>[] eleType =  sih.elementType();
				Class<?> rawType = p.getType();
				if (Collection.class.isAssignableFrom(rawType) || Map.class.isAssignableFrom(rawType)) {
					if(pType!=Void.class&&!rawType.isAssignableFrom(pType)) {
						throw new EcmException(String.format("方法返回集合时注解CjStubReturn定义的type不是方法返回类型或其派生类型", args));
					}
				}
				
				if (pType.equals(Void.class)) {
					pType = rawType;
				}
				args[i] = convertFrom(pType,eleType,value,String.format("方法：%s,参数：%s", src,p.getName()));
				continue;
			}
			CjStubInParameter sip = p.getAnnotation(CjStubInParameter.class);
			if (sip != null) {
				String value = frame.parameter(sip.key());
				try {
					if (!StringUtil.isEmpty(value)) {
						value = URLDecoder.decode(value, "utf-8");
					}
				} catch (UnsupportedEncodingException e) {
				}
				Class<?> pType = sip.type();
				Class<?> eleType[] = sip.elementType();
				Class<?> rawType = p.getType();
				if (Collection.class.isAssignableFrom(rawType) || Map.class.isAssignableFrom(rawType)) {
					if(pType!=Void.class&&!rawType.isAssignableFrom(pType)) {
						throw new EcmException(String.format("方法返回集合时注解CjStubReturn定义的type不是方法返回类型或其派生类型", args));
					}
				}
				if (pType.equals(Void.class)) {
					pType = rawType;
				}
				args[i] = convertFrom(pType,eleType, value,String.format("方法：%s,参数：%s", src,p.getName()));
				continue;
			}
			CjStubInContentKey sic = p.getAnnotation(CjStubInContentKey.class);
			if (sic != null) {
				if (!postContent.containsKey(sic.key())) {
					throw new CircuitException("503", "缺少key在内容。key:" + sic.key() + " 方法：" + src);
				}
				Object tmp = postContent.get(sic.key());
				String json = "";
				if (tmp instanceof String) {
					json = (String) tmp;
					Class<?> pType = sic.type();
					Class<?> eleType[] = sic.elementType();
					Class<?> rawType = p.getType();
					if (Collection.class.isAssignableFrom(rawType) || Map.class.isAssignableFrom(rawType)) {
						if(pType!=Void.class&&!rawType.isAssignableFrom(pType)) {
							throw new EcmException(String.format("方法返回集合时注解CjStubReturn定义的type不是方法返回类型或其派生类型", args));
						}
					}
					if (pType.equals(Void.class)) {
						pType = rawType;
					}
					Object value = convertFrom(pType,eleType, json,String.format("方法：%s,参数：%s", src,p.getName()));
					
					args[i] = value;
				} else {
					if (tmp != null) {
						json = new Gson().toJson(tmp);
						Class<?> pType = sic.type();
						Class<?> eleType[] = sic.elementType();
						Class<?> rawType = p.getType();
						if (Collection.class.isAssignableFrom(rawType) || Map.class.isAssignableFrom(rawType)) {
							if(pType!=Void.class&&!rawType.isAssignableFrom(pType)) {
								throw new EcmException(String.format("方法返回集合时注解CjStubReturn定义的type不是方法返回类型或其派生类型", args));
							}
						}
						if (pType.equals(Void.class)) {
							pType = rawType;
						}
						Object value = convertFrom(pType,eleType, json,String.format("方法：%s,参数：%s", src,p.getName()));
						args[i] = value;
					} else {
						if (p.getType().isPrimitive()) {
							throw new CircuitException("503", "必须为基本型参数赋值。key in content:" + sic.key() + " 方法：" + src);
						}
						args[i] = null;
					}
				}

				continue;
			}
		}

		return args;
	}

	private Method findDestMethod(Class<?> clazz, Method src) throws NoSuchMethodException, SecurityException {
		Method m = null;
		try {
			m = clazz.getDeclaredMethod(src.getName(), src.getParameterTypes());
		} catch (NoSuchMethodException e) {
			if (!Object.class.equals(clazz)) {
				Class<?> superC = clazz.getSuperclass();
				m = findDestMethod(superC, src);
			}
		}
		return m;
	}

}
