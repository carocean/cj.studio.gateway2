package cj.test.website2.valve;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.socket.pipeline.IAnnotationInputValve;
import cj.studio.gateway.socket.pipeline.IIPipeline;

@CjService(name="InputValve1",scope=Scope.multiton)
public class InputValve1 implements IAnnotationInputValve{
	
	@Override
	public void onActive(String inputName,  IIPipeline pipeline)
			throws CircuitException {
		System.out.println("----onActive");
		pipeline.nextOnActive(inputName, this);
	}

	@Override
	public void flow(Object request, Object response, IIPipeline pipeline) throws CircuitException {
		if(request instanceof Frame) {
			Frame f=(Frame)request;
			String url=f.url();
			try {
				url = URLDecoder.decode(url, "utf-8");// 为了不影响性能，资源中文名乱码问题留给应用开发者在valve中处理。
			} catch (UnsupportedEncodingException e1) {
			}
			f.url(url);
		}
		pipeline.nextFlow(request, response, this);
	}

	@Override
	public void onInactive(String inputName, IIPipeline pipeline) throws CircuitException {
		System.out.println("----onInactive");
		pipeline.nextOnInactive(inputName, this);
	}

	@Override
	public int getSort() {
		return 1;
	}

}
