package cj.studio.gateway.server.initializer;

import cj.studio.ecm.IServiceProvider;
import cj.studio.gateway.server.handler.TcpChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class TcpChannelInitializer extends ChannelInitializer<SocketChannel> {
	IServiceProvider parent;

	public TcpChannelInitializer(IServiceProvider parent) {
		this.parent = parent;
	}
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4))
				.addLast(new TcpChannelHandler(parent));
	}


}
