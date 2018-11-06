package cj.studio.gateway.socket.pipeline;

import cj.studio.ecm.graph.CircuitException;

public interface IOutputer {
	/**
	 * 发送，有的目标支持晌应，有的不支持
	 * @param request 
	 * @param response
	 * @throws CircuitException 
	 */
	void send(Object request, Object response) throws CircuitException;

	boolean canCloseablePipeline();
	/**
	 * 关闭管道。如果对端是net将会关闭
	 * @throws CircuitException 
	 */
	void closePipeline() throws CircuitException;
	/**
	 * 仅释放管道
	 */
	void releasePipeline()throws CircuitException;
	
}
