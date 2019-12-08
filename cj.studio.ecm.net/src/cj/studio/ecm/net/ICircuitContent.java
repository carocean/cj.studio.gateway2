package cj.studio.ecm.net;

public interface ICircuitContent {
	/**
	 * 获取分段回写器。<br>
	 * 如果当前CircuitContent不支持分段，则返回null
	 * @return
	 */
	ISegmentCircuitContent segment();
	public abstract void writeBytes(byte[] b);
	public abstract void writeBytes(byte[] b, int pos);
	public abstract void writeBytes(byte[] b, int pos, int len);

	/**
	 * 是否支持内容All-in内存模式
	 * @return
	 */
	boolean isAllInMemory();
	/**
	 * 缓冲区容量。默认8K
	 * 
	 * @return
	 */
	public abstract int capacity();// buf容量
	public void writer(IContentWriter writer);
	/**
	 * 刷新缓冲输出到客户端<br>
	 * 可以调用多次，第一次调用仅将响应头输出客户端，之后是纯数据的输出。如果开发者无调用，则系统在执行完flow方法后自动调用。
	 */
	void flush();

	/**
	 * 是否已提交响应头
	 * 
	 * @return
	 */
	boolean isCommited();

	/**
	 * 是否已准备好，初始态即准备好。
	 * 
	 * @return
	 */
	boolean isReady();

	/**
	 * 如果已完成一次请求的生命周期则返回true
	 * 
	 * @return
	 */
	boolean isClosed();

	public abstract void close();
	/**
	 * 阻塞主线程等待关闭
	 */
	void waitClose(long micSeconds);
	/**
	 * 阻塞主线程等待关闭
	 */
	void waitClose();
	/**
	 * 已经写入的字节数
	 * 
	 * @return
	 */
	public abstract long writedBytes();
	void beginWait();
	/**
	 * 内容容量，超出此容量则页面提交。
	 * @param capacity
	 */
	void setCapacity(int capacity);
	public abstract void clearbuf();

}