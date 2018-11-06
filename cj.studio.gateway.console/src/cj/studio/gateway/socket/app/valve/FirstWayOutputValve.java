package cj.studio.gateway.socket.app.valve;

import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.pipeline.IOPipeline;
import cj.studio.gateway.socket.pipeline.IOutputValve;
import cj.studio.gateway.socket.pipeline.OutputPipelineCollection;

public class FirstWayOutputValve implements IOutputValve{
	OutputPipelineCollection pipelines;//见outputSelector
	public FirstWayOutputValve(OutputPipelineCollection pipelines) {
		this.pipelines=pipelines;
	}

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
		String name=pipeline.prop("To-Name");
		pipeline.nextOnInactive(this);
		pipelines.remove(name);
	}

}
