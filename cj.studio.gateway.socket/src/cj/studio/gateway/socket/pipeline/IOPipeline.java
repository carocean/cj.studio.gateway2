package cj.studio.gateway.socket.pipeline;

import cj.studio.ecm.graph.CircuitException;

public interface IOPipeline {
	void nextFlow(Object request, Object response, IOutputValve formthis) throws CircuitException;

	void nextOnActive(IOutputValve formthis) throws CircuitException;

	/**
	 * 关闭输出通道，最后一个vavle要实现icloseable接口，以关闭channel
	 * 
	 * @throws CircuitException
	 */
	void nextOnInactive(IOutputValve formthis) throws CircuitException;


	String prop(String name);
}
