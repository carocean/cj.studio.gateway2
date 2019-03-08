package cj.studio.gateway.socket.pipeline;

import cj.studio.ecm.net.CircuitException;
/**
 * 输出端子<br>
 * 注意：一定要释放，否则会占用管道资源
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
	 * 关闭管道。不论对端是net channel socket或者net destination socket都会关闭整个socket,即关闭物理连接。
	 * @throws CircuitException 
	 */
	void closePipeline() throws CircuitException;
	/**
	 * 
	 * 释放管道,仅仅释放与网关前后的套节字之间的连接，并不关闭物理管道。网关前套节字：net channel socket；网关后套节字：net destination socket<br>
	 */
	void releasePipeline()throws CircuitException;
	boolean isDisposed();
	
}
