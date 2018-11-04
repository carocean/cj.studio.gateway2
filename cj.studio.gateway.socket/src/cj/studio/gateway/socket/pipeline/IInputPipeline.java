package cj.studio.gateway.socket.pipeline;

import cj.studio.ecm.graph.CircuitException;
import cj.ultimate.IDisposable;

public interface IInputPipeline extends IIPipeline,IDisposable{
	void headFlow(Object request,Object response)throws CircuitException;

	void headOnActive(String inputName,Object request, Object response)throws CircuitException;

	void headOnInactive(String inputName)throws CircuitException;
	 void add(IInputValve valve);
	 void remove(IInputValve valve);
}
