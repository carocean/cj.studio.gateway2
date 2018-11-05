package cj.studio.gateway.socket.app.valve;

import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.pipeline.IOPipeline;
import cj.studio.gateway.socket.pipeline.IOutputValve;

public class FirstWayOutputValve implements IOutputValve{

	@Override
	public void flow(Object request, Object response, IOPipeline pipeline) throws CircuitException {
		pipeline.nextFlow(request, response, this);
	}

}
