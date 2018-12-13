package cj.studio.gateway.socket.pipeline;

import cj.studio.ecm.net.CircuitException;

public interface IIPipeline {
	void nextFlow(Object request,Object response, IInputValve formthis) throws CircuitException;
	void nextOnActive(String inputName, IInputValve formthis)throws CircuitException;

	void nextOnInactive(String inputName, IInputValve formthis) throws CircuitException;
	String prop(String name);
}
