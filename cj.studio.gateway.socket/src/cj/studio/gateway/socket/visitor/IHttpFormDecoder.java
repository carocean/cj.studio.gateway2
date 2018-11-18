package cj.studio.gateway.socket.visitor;

import io.netty.buffer.ByteBuf;

public interface IHttpFormDecoder {

	void done(IHttpWriter writer);

	void writeChunk(ByteBuf chunk);

}
