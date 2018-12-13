package cj.studio.gateway.socket.pipeline;

import cj.studio.ecm.net.CircuitException;
/**
 * backward输出端子<br>
 * 注意：一定要释放，否则会占用backward连结点资源
 * @author caroceanjofers
 *
 */
public interface IOutputer{
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
	 * 关闭管道。不论对端是net channel socket或者net destination socket都会关闭整个socket，后者还会关闭所有物理连接,因此要慎用
	 * @throws CircuitException 
	 */
	void closePipeline() throws CircuitException;
	/**
	 * 释放管道，如果对端是net channel socket则仅释放与之连接的管道，如果是net destination socket则仅是将导线还给电览，并不真实关闭物理连接
	 */
	void releasePipeline()throws CircuitException;
	
}
