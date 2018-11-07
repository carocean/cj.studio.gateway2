package cj.studio.gateway.socket.pipeline;

import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;

public interface IOutputSelector {
	/**
	 * 选择向指定侦来源输出的输出器
	 * @param frame
	 * @return
	 * @throws CircuitException 
	 */
	IOutputer select(Frame frame) throws CircuitException;
	/**
	 * 选择指定目标的输出器
	 * @param name
	 * @return
	 */
	IOutputer select(String name)throws CircuitException;

}
