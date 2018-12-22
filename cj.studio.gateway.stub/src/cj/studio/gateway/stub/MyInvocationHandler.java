package cj.studio.gateway.stub;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IInputChannel;
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.ecm.net.io.MemoryInputChannel;
import cj.studio.ecm.net.io.MemoryOutputChannel;
import cj.studio.gateway.socket.pipeline.IOutputSelector;
import cj.studio.gateway.socket.pipeline.IOutputer;
import cj.studio.gateway.socket.util.SocketContants;
import cj.studio.gateway.stub.annotation.CjStubInContent;
import cj.studio.gateway.stub.annotation.CjStubInHead;
import cj.studio.gateway.stub.annotation.CjStubInParameter;
import cj.studio.gateway.stub.annotation.CjStubMethod;
import cj.studio.gateway.stub.annotation.CjStubRef;
import cj.studio.gateway.stub.annotation.CjStubReturn;
import cj.studio.gateway.stub.annotation.CjStubService;
import cj.studio.gateway.stub.util.StringTypeConverter;
import cj.ultimate.gson2.com.google.gson.Gson;
import cj.ultimate.net.sf.cglib.proxy.InvocationHandler;

public class MyInvocationHandler implements InvocationHandler, StringTypeConverter {
	IOutputSelector selector;
	CjStubService stubService;
	private String contentPath;
	String stubClassName;
	private String dest;
	ThreadLocal<IOutputer> local;

	public MyInvocationHandler(IOutputSelector selector, String remote, Class<?> stub) throws CircuitException {
		if (remote.startsWith("rest://")) {
			dest = remote.substring("rest:/".length(), remote.length());
		} else {
			dest = remote;
		}
		this.selector = selector;
		this.stubService = stub.getDeclaredAnnotation(CjStubService.class);
		this.contentPath = getContentPath(dest);
		stubClassName = stub.getName();
		this.local = new ThreadLocal<>();
	}

	public MyInvocationHandler(IOutputSelector selector, CjStubRef sr) throws CircuitException {
		if (sr.remote().startsWith("rest://")) {
			dest = sr.remote().substring("rest:/".length(), sr.remote().length());
		} else {
			dest = sr.remote();
		}
		this.selector = selector;
		this.stubService = sr.stub().getAnnotation(CjStubService.class);
		contentPath = getContentPath(dest);
		stubClassName = sr.stub().getName();
		this.local = new ThreadLocal<>();
	}

	private String getContentPath(String dest) throws CircuitException {
		boolean isStarts = false;
		while (dest.startsWith("/")) {
			dest = dest.substring(1, dest.length());
			isStarts = true;
		}
		if (!isStarts) {
			throw new CircuitException("503", "目标格式错误，没有以/开头:" + dest);
		}
		int pos = dest.indexOf("/");
		if (pos < 0) {
			throw new CircuitException("503", "目标格式错误，没有指定要访问的后端应用上下文:" + dest);
		}
		String contentPath = dest.substring(pos, dest.length());
		return contentPath;
	}

	@Override
	public Object invoke(Object obj, Method m, Object[] args) throws Throwable {
		IOutputer out = local.get();
		if (out != null) {
			return out;
		}
		try {
			out = selector.select(this.dest);
			local.set(out);
			return invoke(out, obj, m, args);
		} catch (Exception e) {
			throw e;
		} finally {
			if (out != null) {
				local.remove();
				out.closePipeline();
			}
		}

	}

	private Object invoke(IOutputer out, Object obj, Method m, Object[] args) throws Exception {
		CjStubMethod sm = m.getDeclaredAnnotation(CjStubMethod.class);
		if (sm == null) {
			throw new Exception("缺少存根方法注解:" + m);
		}
		String name = sm.alias();
		if (name.indexOf("/") > -1) {
			throw new Exception("CjStubMethod注解错误，别名不能含有/。在：" + m);
		}
		String uri = contentPath;
		if (uri.endsWith("/")) {
			if (stubService.bindService().startsWith("/")) {
				String sub = stubService.bindService();
				while (sub.startsWith("/")) {
					sub = sub.substring(1, sub.length());
				}
				uri += sub;
			} else {
				uri += "/" + stubService.bindService();
			}
		} else {
			if (stubService.bindService().startsWith("/")) {
				uri += stubService.bindService();
			} else {
				uri += "/" + stubService.bindService();
			}
		}
		if (!uri.endsWith("/")) {
			uri += "/";
		}

		String fline = String.format("%s %s %s", sm.command(), uri, sm.protocol());
		IInputChannel ic = new MemoryInputChannel();
		Frame frame = new Frame(ic, fline);
		MemoryContentReciever mcr = new MemoryContentReciever();
		frame.content().accept(mcr);
		frame.head(SocketContants.__frame_Head_Rest_Command, name);
		frame.head(SocketContants.__frame_Head_Rest_Stub_Interface, stubClassName);

		Annotation[][] arr = m.getParameterAnnotations();
		for (int i = 0; i < arr.length; i++) {
			Annotation[] argAnnos = arr[i];
			if (argAnnos.length < 1)
				continue;
			// 方法参数的注解目前只支持一个
			for (Annotation a : argAnnos) {
				fillFrame(frame, a, i, args, ic, sm);
			}

		}
		String sline = String.format("%s 200 OK", sm.protocol());
		MemoryOutputChannel oc = new MemoryOutputChannel();
		Circuit circuit = new Circuit(oc, sline);

		out.send(frame, circuit);
		int state = Integer.valueOf(circuit.status());
		if (state >= 400) {
			throw new CircuitException(circuit.status(), circuit.message());
		}
		if (m.getReturnType() == null) {
			return null;
		}
		circuit.content().close();
		byte[] b = oc.readFully();
		CjStubReturn sr = m.getDeclaredAnnotation(CjStubReturn.class);
		if (!m.getReturnType().equals(Void.TYPE) && sr == null) {
			throw new EcmException("缺少CjStubReturn注解。在：" + m);
		} else {
			String feed = new String(b);
			Object ret = convertFrom(m.getReturnType(), feed);
			return ret;
		}

	}

	private void fillFrame(Frame frame, Annotation a, int i, Object[] args, IInputChannel ic, CjStubMethod sm)
			throws CircuitException {
		if (a instanceof CjStubInHead) {
			CjStubInHead sih = (CjStubInHead) a;
			if (sih != null) {
				String key = sih.key();
				String v = "";
				if (args[i].getClass().isPrimitive()) {
					v = String.valueOf(args[i]);
				} else {
					v = new Gson().toJson(args[i]);
				}
				if (v.indexOf("\r") > -1 || v.indexOf("\n") > -1) {
					throw new EcmException("含有\r或\n");
				}
				frame.head(key, v);
			}
		} else if (a instanceof CjStubInParameter) {
			CjStubInParameter sip = (CjStubInParameter) a;
			if (sip != null) {
				String key = sip.key();
				String v = "";
				if (args[i].getClass().equals(String.class)) {
					v = (String) args[i];
				} else if (args[i].getClass().isPrimitive()) {
					v = String.valueOf(args[i]);
				} else {
					v = new Gson().toJson(args[i]);
				}
				if (v.indexOf("\r") > -1 || v.indexOf("\n") > -1) {
					throw new EcmException("含有\r或\n");
				}
				frame.parameter(key, v);
			}
		} else if (a instanceof CjStubInContent) {
			CjStubInContent sic = (CjStubInContent) a;
			if (sic != null) {
				if (!"POST".equalsIgnoreCase(sm.command())) {
					throw new EcmException("有内容且command不是post");
				}
				String v = "";
				if (args[i].getClass().isPrimitive()) {
					v = String.valueOf(args[i]);
				} else {
					v = new Gson().toJson(args[i]);
				}
				byte[] b = v.getBytes();
				ic.begin(frame);
				ic.done(b, 0, b.length);
			}
		}
	}

}