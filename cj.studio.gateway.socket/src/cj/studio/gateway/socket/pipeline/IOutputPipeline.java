package cj.studio.gateway.socket.pipeline;

import cj.studio.ecm.net.CircuitException;
import cj.ultimate.IDisposable;

public interface IOutputPipeline extends IOPipeline,IDisposable{
	void headFlow(Object request,Object response)throws CircuitException;
	void headOnActive()throws CircuitException;
	void headOnInactive()throws CircuitException;
	IOutputer handler();
	void prop(String name,String value);

	void add(IOutputValve valve);

	void remove(IOutputValve valve);

	boolean isDisposed();
	void dispose(boolean disposeCloseableOutputValve);
}
