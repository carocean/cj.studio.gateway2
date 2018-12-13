package cj.studio.gateway.socket.io;

import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IInputChannel;

public interface IFeedback {

	Frame createFirst(String frame_line) throws CircuitException;

	void commitFirst(Frame first)throws CircuitException;

	IInputChannel createContent()throws CircuitException;

	void commitContent(IInputChannel in)throws CircuitException;

	IInputChannel createLast()throws CircuitException;

	void commitLast(IInputChannel in)throws CircuitException;

}
