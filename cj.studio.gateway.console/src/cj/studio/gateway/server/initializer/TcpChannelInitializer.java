package cj.studio.gateway.server.initializer;

import java.util.concurrent.TimeUnit;

import cj.studio.ecm.IServiceProvider;
import cj.studio.gateway.conf.ServerInfo;
import cj.studio.gateway.server.handler.TcpChannelHandler;
import cj.ultimate.util.StringUtil;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

public class TcpChannelInitializer extends ChannelInitializer<SocketChannel> {
	IServiceProvider parent;

	public TcpChannelInitializer(IServiceProvider parent) {
		this.parent = parent;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast(new LengthFieldBasedFrameDecoder(81920, 0, 4, 0, 4));
		
		ServerInfo info = (ServerInfo) parent.getService("$.server.info");
		String interval = info.getProps().get("heartbeat");
		if (!StringUtil.isEmpty(interval)) {
			int hb=Integer.valueOf(interval);
			if(hb<=0) {
				hb=10;
			}
			pipeline.addLast(new IdleStateHandler(hb, 0, 0, TimeUnit.SECONDS));
		}
		pipeline.addLast(new TcpChannelHandler(parent));
	}

}
