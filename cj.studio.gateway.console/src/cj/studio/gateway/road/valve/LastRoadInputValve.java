package cj.studio.gateway.road.valve;

import java.util.List;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.road.http.HttpBackendHandler;
import cj.studio.gateway.socket.Destination;
import cj.studio.gateway.socket.pipeline.IIPipeline;
import cj.studio.gateway.socket.pipeline.IInputValve;
import cj.studio.gateway.socket.pipeline.IOutputPipeline;
import cj.studio.gateway.socket.pipeline.IOutputPipelineBuilder;
import cj.studio.gateway.socket.util.HashFunction;
import cj.studio.gateway.socket.util.SocketContants;
import cj.studio.gateway.socket.util.SocketName;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.stream.ChunkedWriteHandler;

public class LastRoadInputValve implements IInputValve {
	IServiceProvider parent;
	Destination dest;
	Channel frontend;
	Channel backend;
	private String host;

	public LastRoadInputValve(IServiceProvider parent, Channel frontend) {
		this.parent = parent;
		this.dest = (Destination) parent.getService("$.destination");
		this.frontend = frontend;
	}

	@Override
	public void onActive(String inputName, IIPipeline pipeline) throws CircuitException {
		// 连接远程
		Bootstrap b = new Bootstrap();
		b.group(frontend.eventLoop()).channel(frontend.getClass()).handler(new MyChannelInitializer())
				.option(ChannelOption.AUTO_READ, false);
		List<String> uris = dest.getUris();
		int index = (int) (Math.abs(new HashFunction().hash(inputName)) % uris.size());
		String uri = uris.get(index);
		int pos = uri.indexOf("://");
//		String protocol = uri.substring(0, pos);
		String domain = uri.substring(pos + 3, uri.length());
		pos = domain.indexOf(":");
		String remoteHost = "";
		int remotePort = 0;
		if (pos > 0) {
			remoteHost = domain.substring(0, pos);
			remotePort = Integer.valueOf(domain.substring(pos + 1, domain.length()));
		} else {
			remoteHost = domain;
			remotePort = 80;
		}
		this.host = String.format("%s:%s", remoteHost, remotePort);
		ChannelFuture f = b.connect(remoteHost, remotePort);
		backend = f.channel();
		f.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					frontend.read();
				} else {
					frontend.close();
				}
			}
		});
	}

	@Override
	public void flow(Object request, Object ctx, IIPipeline pipeline) throws CircuitException {
		if (backend.isActive()) {
			if (request instanceof HttpRequest) {
				HttpRequest req = (HttpRequest) request;
				req.headers().set("Host", host);
			}
			backend.writeAndFlush(request).addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (future.isSuccess()) {
						// was able to flush out data, start to read the next chunk
						frontend.read();
					} else {
						future.channel().close();
					}
				}
			});
		}
	}

	@Override
	public void onInactive(String inputName, IIPipeline pipeline) throws CircuitException {
		// 关闭远程
		if (backend != null) {
			closeOnFlush(backend);
		}
	}

	/**
	 * Closes the specified channel after all queued write requests are flushed.
	 */
	static void closeOnFlush(Channel ch) {
		if (ch.isActive()) {
			ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
		}
	}

	class MyChannelInitializer extends ChannelInitializer<SocketChannel> {

		public MyChannelInitializer() {
		}

		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			ChannelPipeline pipeline = ch.pipeline();

			pipeline.addLast("codec", new HttpClientCodec());

			// Remove the following line if you don't want automatic content decompression.
			pipeline.addLast("inflater", new HttpContentDecompressor());
			pipeline.addLast("chunked", new ChunkedWriteHandler());
			IOutputPipelineBuilder buidler = (IOutputPipelineBuilder) parent.getService("$.pipeline.output.builder");
			String netname = (String) parent.getService("$.socket.name");
			String name = SocketName.name(ch.id(), netname);
			IOutputPipeline output = buidler.name(name)
					.prop(SocketContants.__pipeline_builder_frontend_channel, frontend).prop("__pipeline_builder_backend_channel", ch)
					.createPipeline();
			pipeline.addLast("handler", new HttpBackendHandler(output));

		}

	}

}
