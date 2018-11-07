package cj.studio.gateway.socket.pipeline;

import cj.studio.ecm.graph.CircuitException;

public interface IIPipeline {
	void nextFlow(Object request,Object response, IInputValve formthis) throws CircuitException;
	void nextOnActive(String inputName,Object request, Object response, IInputValve formthis)throws CircuitException;

	void nextOnInactive(String inputName, IInputValve formthis) throws CircuitException;
	String prop(String name);
}
