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
import io.netty.handler.codec.http.HttpObjectAggregator;
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
		pipeline.addLast( new HttpServerCodec());
		pipeline.addLast(new HttpContentCompressor());
		pipeline.addLast( new ChunkedWriteHandler());
		HttpChannelHandler handler = new HttpChannelHandler(parent);
		pipeline.addLast(handler);
	}

	// netty铁定响应头仅支持assic码，所以重写了这个类，试了几个浏览器，仅有chrome浏览器的头信息能支持中文，因此还是改为标准的http响应码消息表吧。
	// 在使用本类时需注释掉：pipeline.addLast("codec-http", new HttpServerCodec(4096, 8192,
	// SocketContants.__upload_chunked_cache_size));
	// 添加（HttpServerCodec作用等同于下面的两个）：
//	pipeline.addLast(new HttpRequestDecoder(4096, 8192, SocketContants.__upload_chunked_cache_size));
//	pipeline.addLast(new MyHttpResponseEncoder());
//	class MyHttpResponseEncoder extends HttpResponseEncoder {
//		@Override
//		protected void encodeInitialLine(ByteBuf buf, HttpResponse response) throws Exception {
//			buf.writeBytes(response.getProtocolVersion().text().getBytes());
//			buf.writeByte(' ');
//			buf.writeBytes(response.getStatus().toString().getBytes());
//			buf.writeBytes(new byte[] { '\r', '\n' });
//		}
//	}
}