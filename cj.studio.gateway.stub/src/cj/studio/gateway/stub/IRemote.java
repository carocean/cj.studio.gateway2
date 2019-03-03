package cj.studio.gateway.stub;

import cj.studio.ecm.net.CircuitException;

public interface IRemote {

	<T>T open(Class<T> stub) throws CircuitException;
	/**
	 * 
	 * @param stub
	 * @param aync 
	 * 是否是异步调用存根。true为异步。<br>
	 * -同步调用存根仅支持http协议，异步调用支持tcp|udt|ws协议
	 * <br>
	 * -异步调用方法无返回值，或永远返回null
	 * <br>
	 * <b>默认为同步调用</b>
	 * @return
	 * @throws CircuitException
	 */
	<T>T open(Class<T> stub,boolean aync) throws CircuitException;

}
