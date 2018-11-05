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

	/**
	 * 关闭管道。如果对端是net将会关闭
	 */
	void closePipeline();
	/**
	 * 仅释放管道
	 */
	void releasePipeline();
	
}
