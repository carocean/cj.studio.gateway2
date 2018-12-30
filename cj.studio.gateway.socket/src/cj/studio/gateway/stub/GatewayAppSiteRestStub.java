package cj.studio.gateway.stub;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URLDecoder;
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
import cj.studio.gateway.stub.annotation.CjStubInContent;
import cj.studio.gateway.stub.annotation.CjStubInHead;
import cj.studio.gateway.stub.annotation.CjStubInParameter;
import cj.studio.gateway.stub.annotation.CjStubMethod;
import cj.studio.gateway.stub.annotation.CjStubService;
import cj.studio.gateway.stub.util.StringTypeConverter;
import cj.ultimate.gson2.com.google.gson.Gson;
import cj.ultimate.gson2.com.google.gson.reflect.TypeToken;
import cj.ultimate.util.StringUtil;

public class GatewayAppSiteRestStub implements IGatewayAppSiteWayWebView, StringTypeConverter {
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
					Method src =__stubMethods.get(restCmd);
					if (src == null) {
						throw new CircuitException("404", "在存根接口中未找到方法：" + src);
					}
					Method dest = findDestMethod(clazz, src);
					if (dest == null) {
						throw new CircuitException("404", "在webview中未找到方法：" + dest);
					}
					Object[] args = getArgs(src, frame);
					Object ret = dest.invoke(GatewayAppSiteRestStub.this, args);
					if (ret != null) {
						circuit.content().writeBytes(new Gson().toJson(ret).getBytes());
					}
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

	private Object[] getArgs(Method src, Frame frame) throws CircuitException {
		Map<String, String> postContent = null;
		if ("post".equalsIgnoreCase(frame.command())) {
			byte[] b = frame.content().readFully();
			postContent = new Gson().fromJson(new String(b), new TypeToken<HashMap<String, String>>() {
			}.getType());
		}
		Parameter[] arr = src.getParameters();
		Object[] args = new Object[arr.length];
		boolean hasContent = false;
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
				args[i] = convertFrom(p.getType(), value);
				continue;
			}
			CjStubInParameter sip = p.getAnnotation(CjStubInParameter.class);
			if (sip != null) {
				if (postContent != null) {
					String value = postContent.get(sip.key());
					args[i] = convertFrom(p.getType(), value);
				} else {
					String value = frame.parameter(sip.key());
					try {
						if (!StringUtil.isEmpty(value)) {
							value = URLDecoder.decode(value, "utf-8");
						}
					} catch (UnsupportedEncodingException e) {
					}
					args[i] = convertFrom(p.getType(), value);
				}
				continue;
			}
			CjStubInContent sic = p.getAnnotation(CjStubInContent.class);
			if (sic != null) {
				if (hasContent) {
					throw new CircuitException("503", "存在多个内容注解CjStubInContent在方法：" + src);
				}
				String json = postContent.get("^content$");
				Object value = new Gson().fromJson(json, p.getType());
				args[i] = value;
				hasContent = true;
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

//	private Method findMethod(String restCmd, Class<?> stub) {
//		Method[] arr = stub.getDeclaredMethods();
//		for (Method m : arr) {
//			CjStubMethod cm = m.getAnnotation(CjStubMethod.class);
//			if (cm == null) {
//				continue;
//			}
//			String methodName = cm.alias();
//			if (StringUtil.isEmpty(methodName)) {
//				methodName = m.getName();
//			}
//			if (restCmd.equals(methodName)) {
//				return m;
//			}
//		}
//		return null;
//	}
}
