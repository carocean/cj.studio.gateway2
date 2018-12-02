package cj.studio.gateway.socket.visitor;

import io.netty.buffer.ByteBuf;

public interface IHttpFormChunkDecoder {

	void done(IHttpVisitorWriter writer);

	void writeChunk(ByteBuf chunk);

}
