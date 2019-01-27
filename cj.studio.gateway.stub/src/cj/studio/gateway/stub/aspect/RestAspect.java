package cj.studio.gateway.stub.aspect;

import java.lang.reflect.Field;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.adapter.IAdaptable;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.bridge.IAspect;
import cj.studio.ecm.bridge.ICutpoint;
import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.socket.pipeline.IOutputSelector;
import cj.studio.gateway.stub.MyInvocationHandler;
import cj.studio.gateway.stub.annotation.CjStubRef;
import cj.ultimate.net.sf.cglib.proxy.Enhancer;

@CjService(name = "@rest")
public class RestAspect implements IAspect {
	@CjServiceRef(refByName = "$.output.selector")
	IOutputSelector selector;

	@Override
	public Object cut(Object obj, Object[] args, ICutpoint cut) throws Throwable{
		return cut.cut(obj, args);
	}

	@Override
	public Class<?>[] getCutInterfaces() {
		return new Class<?>[] { IGatewayAppSiteWayWebView.class };
	}

	@Override
	public void observe(Object service) {
		Class<?> c = service.getClass();
		do {
			Field[] arr = c.getDeclaredFields();
			for (Field f : arr) {
				CjStubRef sr = f.getAnnotation(CjStubRef.class);
				if (sr == null) {
					continue;
				}
				try {
					Object stub = createStub(sr);
					f.setAccessible(true);
					f.set(service, stub);
				} catch (Exception e) {
					CJSystem.logging().error(getClass(), e);
				}
			}
			c = c.getSuperclass();
		} while (!Object.class.equals(c));
	}

	private Object createStub(CjStubRef sr) throws CircuitException {
		// 实现代理
		Enhancer en = new Enhancer();
		en.setClassLoader(sr.stub().getClassLoader());
		en.setSuperclass(Object.class);
		en.setInterfaces(new Class<?>[] { sr.stub(), IAdaptable.class });
		en.setCallback(new MyInvocationHandler(selector, sr));
		return  en.create();
	}
	
	

}
