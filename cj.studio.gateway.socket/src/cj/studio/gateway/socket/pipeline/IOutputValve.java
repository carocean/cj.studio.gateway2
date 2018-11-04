package cj.studio.gateway.socket.pipeline;


import cj.studio.ecm.graph.CircuitException;

public interface IOutputValve {

	void flow(Object request,Object response, IOPipeline pipeline)throws CircuitException;

}
