package cj.studio.gateway.server.initializer;

import javax.net.ssl.SSLEngine;

import cj.studio.ecm.IServiceProvider;
import cj.studio.gateway.server.handler.HttpChannelHandler;
import cj.studio.gateway.server.secure.ISSLEngineFactory;
import cj.studio.gateway.socket.util.SocketContants;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class HttpChannelInitializer extends ChannelInitializer<SocketChannel> {
	IServiceProvider parent;

	public HttpChannelInitializer(IServiceProvider parent) {
		this.parent = parent;

	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
		ISSLEngineFactory factory = (ISSLEngineFactory) parent.getService("$.server.sslEngine");
		if (factory.isEnabled()) {
			SSLEngine engine = factory.createSSLEngine();
			engine.setUseClientMode(false);
			pipeline.addLast("ssl", new SslHandler(engine));
		}
		pipeline.addLast("codec-http", new HttpServerCodec(4096, 8192, SocketContants.__upload_chunked_cache_size));
		pipeline.addLast("deflater", new HttpContentCompressor());
		pipeline.addLast("http-chunked", new ChunkedWriteHandler());
		HttpChannelHandler handler = new HttpChannelHandler(parent);
		pipeline.addLast("handler", handler);
	}
}