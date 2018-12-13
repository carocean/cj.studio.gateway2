package cj.studio.ecm.net;

public interface IFrameContent {
	/**
	 * 是否支持内容所有在内存模式。如果支持则可以用readFully()读出
	 * @return
	 */
	boolean isAllInMemory();
	byte[] readFully()throws CircuitException;
	void accept(IContentReciever reciever) throws CircuitException;

	long revcievedBytes() throws CircuitException;

}
