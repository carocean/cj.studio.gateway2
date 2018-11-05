package cj.test.website;

import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.pipeline.IAnnotationInputValve;
import cj.studio.gateway.socket.pipeline.IIPipeline;

@CjService(name="InputValve2",scope=Scope.multiton)
public class InputValve2 implements IAnnotationInputValve{
	
	@Override
	public void onActive(String inputName, Object request, Object response, IIPipeline pipeline)
			throws CircuitException {
		// TODO Auto-generated method stub
		pipeline.nextOnActive(inputName, request, response, this);
	}

	@Override
	public void flow(Object request, Object response, IIPipeline pipeline) throws CircuitException {
		// TODO Auto-generated method stub
		pipeline.nextFlow(request, response, this);
	}

	@Override
	public void onInactive(String inputName, IIPipeline pipeline) throws CircuitException {
		// TODO Auto-generated method stub
		pipeline.nextOnInactive(inputName, this);
	}

	@Override
	public int getSort() {
		return 2;
	}

}
