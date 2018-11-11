package cj.studio.gateway.server.initializer;

import cj.studio.ecm.IServiceProvider;
import cj.studio.gateway.server.handler.UdtChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.udt.UdtChannel;

public class UdtChannelInitializer extends ChannelInitializer<UdtChannel> {
	IServiceProvider parent;
	public UdtChannelInitializer(IServiceProvider parent) {
		this.parent=parent;
	}
	@Override
	protected void initChannel(UdtChannel ch) throws Exception {
		// DelimiterFrameCodec2());//如果是二进制报文才使用此解码器
		ch.pipeline().addLast(new UdtChannelHandler(parent));
	}

}
