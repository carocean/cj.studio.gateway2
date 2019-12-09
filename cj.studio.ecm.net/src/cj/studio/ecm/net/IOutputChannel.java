package cj.studio.ecm.net;

import io.netty.channel.FileRegion;

public interface IOutputChannel {
	void write(byte[] b,int pos,int length);
	void begin(Circuit circuit);//circuit or frame
	void done(byte[] b,int pos,int length);
	long writedBytes();
	/**
	 * 输出管道是否已关闭。如：对于内存输出管道永远不关闭
	 * @return
	 */
	boolean isClosed();


}
