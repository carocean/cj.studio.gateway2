package cj.studio.gateway.server.initializer;

import cj.studio.ecm.IServiceProvider;
import cj.studio.gateway.server.handler.HttpChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

public class HttpChannelInitializer extends ChannelInitializer<SocketChannel> {
	IServiceProvider parent;

	public HttpChannelInitializer(IServiceProvider parent) {
		this.parent=parent;
		
	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast("codec-http", new HttpServerCodec());
		pipeline.addLast("deflater", new HttpContentCompressor());
		ch.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
		HttpChannelHandler handler = new HttpChannelHandler(parent);
		pipeline.addLast("handler", handler);
	}
}