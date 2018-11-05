package cj.studio.gateway.socket.pipeline;

import cj.studio.ecm.graph.CircuitException;
import cj.ultimate.IDisposable;

public interface IOutputPipeline extends IOPipeline,IDisposable{
	void headFlow(Object request,Object response)throws CircuitException;

	IOutputer handler();
	void prop(String name,String value);

	void add(IOutputValve valve);

	void remove(IOutputValve valve);

	void dispose();
	
}
