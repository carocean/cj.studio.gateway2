package cj.studio.gateway.socket.cable;

import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.ultimate.IClosable;
import cj.ultimate.IDisposable;

/**
 * 导线
 * 
 * <pre>
 * -一个导线对应包装一个物理连结
 * </pre>
 * 
 * @author caroceanjofers
 *
 */
public interface IGatewaySocketWire extends IClosable, IDisposable {
	/**
	 * 不是真实的关闭物理连结，而是将导线生命期交由电缆管理。
	 */
	@Override
	void close();
	/**
	 * 关闭并物理断开连接
	 */
	@Override
	void dispose();

	/**
	 * 发送
	 * 
	 * @param frame 要发送的侦
	 * @return 如果是http则返回的是Circuit，其它协议可能返回null
	 * @throws CircuitException
	 */
	Object send(Frame frame) throws CircuitException;

	void connect(String ip, int port) throws CircuitException;

	boolean isWritable();

	boolean isOpened();

	boolean isIdle();

	long idleBeginTime();
	void used(boolean b);

}
