package cj.test.website2;

import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.http.HttpFrame;
import cj.studio.gateway.socket.pipeline.IAnnotationInputValve;
import cj.studio.gateway.socket.pipeline.IIPipeline;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

@CjService(name="InputValve1",scope=Scope.multiton)
public class InputValve1 implements IAnnotationInputValve{
	
	@Override
	public void onActive(String inputName,  IIPipeline pipeline)
			throws CircuitException {
		// TODO Auto-generated method stub
		pipeline.nextOnActive(inputName, this);
	}

	@Override
	public void flow(Object request, Object response, IIPipeline pipeline) throws CircuitException {
		pipeline.nextFlow(request, response, this);
	}

	@Override
	public void onInactive(String inputName, IIPipeline pipeline) throws CircuitException {
		// TODO Auto-generated method stub
		pipeline.nextOnInactive(inputName, this);
	}

	@Override
	public int getSort() {
		return 0;
	}

}
