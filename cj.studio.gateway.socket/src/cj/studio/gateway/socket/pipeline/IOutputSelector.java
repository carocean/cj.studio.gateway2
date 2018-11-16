package cj.studio.gateway.socket.pipeline;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.IAsynchronizer;

public interface IOutputSelector {
	/**
	 * 选择向指定侦来源输出的输出器
	 * @param frame
	 * @return
	 * @throws CircuitException 
	 */
	IOutputer select(Frame frame) throws CircuitException;
	/**
	 * 异步器<br>
	 * 异步器可以在同一管道下接收块数据
	 * <pre>
	 * - 文件的块读写
	 * - formdata的解析
	 * - ……
	 * </pre>
	 * @param circuit
	 * @return
	 * @throws CircuitException
	 */
	IAsynchronizer select(Circuit circuit) throws CircuitException;
	/**
	 * 选择指定目标的输出器
	 * @param name
	 * @return
	 */
	IOutputer select(String name)throws CircuitException;

}
