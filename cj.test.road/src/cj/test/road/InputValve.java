package cj.test.road;

import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.pipeline.IAnnotationInputValve;
import cj.studio.gateway.socket.pipeline.IIPipeline;
@CjService(name="InputValve1",scope=Scope.multiton)
public class InputValve implements IAnnotationInputValve{

	@Override
	public void onActive(String inputName, IIPipeline pipeline) throws CircuitException {
		// TODO Auto-generated method stub
		System.out.println("---road--InputValve1--onActive");
		pipeline.nextOnActive(inputName, this);
	}

	@Override
	public void flow(Object request, Object response, IIPipeline pipeline) throws CircuitException {
		// TODO Auto-generated method stub
		System.out.println("---road--InputValve1--flow");
		pipeline.nextFlow(request, response, this);
	}

	@Override
	public void onInactive(String inputName, IIPipeline pipeline) throws CircuitException {
		// TODO Auto-generated method stub
		System.out.println("---road--InputValve1--onInactive");
		pipeline.nextOnInactive(inputName, this);
	}

	@Override
	public int getSort() {
		return 0;
	}

}
