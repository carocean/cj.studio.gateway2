package cj.test.website;

import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.pipeline.IAnnotationOutputValve;
import cj.studio.gateway.socket.pipeline.IOPipeline;

@CjService(name="OutputValve2",scope=Scope.multiton)
public class OutputValve2 implements IAnnotationOutputValve{
	
	@Override
	public void flow(Object request, Object response, IOPipeline pipeline) throws CircuitException {
		System.out.println("0000000--OutputValve2---"+request);
		pipeline.nextFlow(request, response, this);
	}
	@Override
	public int getSort() {
		return 1;
	}

}
