package cj.studio.gateway.server.initializer;

import java.util.concurrent.TimeUnit;

import cj.studio.ecm.IServiceProvider;
import cj.studio.gateway.conf.ServerInfo;
import cj.studio.gateway.server.handler.UdtChannelHandler;
import cj.ultimate.util.StringUtil;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.udt.UdtChannel;
import io.netty.handler.timeout.IdleStateHandler;

public class UdtChannelInitializer extends ChannelInitializer<UdtChannel> {
	IServiceProvider parent;

	public UdtChannelInitializer(IServiceProvider parent) {
		this.parent = parent;
	}

	@Override
	protected void initChannel(UdtChannel ch) throws Exception {
		ServerInfo info = (ServerInfo) parent.getService("$.server.info");
		// DelimiterFrameCodec2());//如果是二进制报文才使用此解码器
		String interval = info.getProps().get("heartbeat");
		if (!StringUtil.isEmpty(interval)) {
			int hb = Integer.valueOf(interval);
			if (hb <= 0) {
				hb = 10;
			}
			ch.pipeline().addLast(new IdleStateHandler(hb, 0, 0, TimeUnit.SECONDS));
		}
		ch.pipeline().addLast(new UdtChannelHandler(parent));
	}

}
