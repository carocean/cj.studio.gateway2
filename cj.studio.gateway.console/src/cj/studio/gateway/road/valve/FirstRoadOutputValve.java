package cj.studio.gateway.road.valve;

import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.socket.pipeline.IOPipeline;
import cj.studio.gateway.socket.pipeline.IOutputValve;

public class FirstRoadOutputValve implements IOutputValve {

	@Override
	public void flow(Object request, Object response, IOPipeline pipeline) throws CircuitException {
		pipeline.nextFlow(request, response, this);
	}

	@Override
	public void onActive(IOPipeline pipeline) throws CircuitException {
		pipeline.nextOnActive(this);
	}

	@Override
	public void onInactive(IOPipeline pipeline) throws CircuitException {
		pipeline.nextOnInactive(this);
	}

}
