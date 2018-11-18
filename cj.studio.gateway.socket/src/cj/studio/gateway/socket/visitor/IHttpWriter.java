package cj.studio.gateway.socket.visitor;

import cj.ultimate.IClosable;
import io.netty.buffer.ByteBuf;

public interface IHttpWriter extends IClosable{
	void write(byte[] b);

	void write(ByteBuf buf);

	void write(byte[] b, int offset, int len);
}
