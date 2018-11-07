package cj.studio.gateway.socket.pipeline;

import cj.studio.ecm.graph.CircuitException;
/**
 * backward输出端子<br>
 * 注意：一定要释放，否则会占用backward连结点资源
 * @author caroceanjofers
 *
 */
public interface IOutputer {
	/**
	 * 发送，有的目标支持晌应，有的不支持
	 * @param request 
	 * @param response
	 * @throws CircuitException 
	 */
	void send(Object request, Object response) throws CircuitException;
	/**
	 * 是否可以关闭
	 * @return
	 */
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
