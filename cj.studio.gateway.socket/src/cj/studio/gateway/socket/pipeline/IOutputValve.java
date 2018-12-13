package cj.studio.gateway.socket.pipeline;


import cj.studio.ecm.net.CircuitException;

public interface IOutputValve {

	void flow(Object request,Object response, IOPipeline pipeline)throws CircuitException;

	void onActive(IOPipeline pipeline) throws CircuitException;

	void onInactive(IOPipeline pipeline) throws CircuitException;

}
