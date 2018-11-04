package cj.studio.gateway.socket.pipeline;

import cj.studio.ecm.graph.CircuitException;

public interface IOutputPipeline extends IOPipeline{
	void headFlow(Object request,Object response)throws CircuitException;

	
}
