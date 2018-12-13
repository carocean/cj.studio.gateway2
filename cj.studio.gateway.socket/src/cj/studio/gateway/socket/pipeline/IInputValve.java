package cj.studio.gateway.socket.pipeline;

import cj.studio.ecm.net.CircuitException;

public interface IInputValve {
	void onActive(String inputName,  IIPipeline pipeline) throws CircuitException;

	void flow(Object request, Object response, IIPipeline pipeline) throws CircuitException;

	void onInactive(String inputName, IIPipeline pipeline) throws CircuitException;

}
