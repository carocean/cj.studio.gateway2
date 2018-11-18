package cj.studio.gateway.socket.visitor;

import cj.studio.gateway.socket.IChunkVisitor;
import cj.ultimate.IClosable;

public abstract class AbstractHttpGetVisitor implements IChunkVisitor ,IClosable{

	/**
	 * 读块
	 * @param b
	 * @param i
	 * @param length
	 * @return 返回-1表示结束
	 */
	public abstract  int readChunk(byte[] b, int i, int length);

	/**
	 * 内容长度，即所有读取块的长度
	 * @return
	 */
	public abstract long getContentLength() ;
	/**
	 * 关闭文件
	 */
	@Override
	public abstract void close();
	
	

}
