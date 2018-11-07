package cj.studio.gateway.server.initializer;

import cj.studio.ecm.IServiceProvider;
import cj.studio.gateway.conf.ServerInfo;
import cj.studio.gateway.server.handler.WebsocketServerHandler;
import cj.studio.gateway.socket.util.SocketContants;
import cj.ultimate.util.StringUtil;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

public class WebsocketChannelInitializer extends ChannelInitializer<SocketChannel> {
	IServiceProvider parent;

	public WebsocketChannelInitializer(IServiceProvider parent) {
		this.parent = parent;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		// 未认证通过则：
		// handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame);
		// WebSocketFrameAggregator
		ServerInfo info = (ServerInfo) parent.getService("$.server.info");
		String aggregatorstr = info.getProps().get(SocketContants.__http_ws_prop_aggregatorFileLengthLimit);// 最大聚合的内容大小，一般用于限制文件最大大小的上传
		if (StringUtil.isEmpty(aggregatorstr)) {
			aggregatorstr = "10485760";// 默认是10M
		}
		int aggregator = Integer.valueOf(aggregatorstr);
		String path = info.getProps().get(SocketContants.__http_ws_prop_wsPath);
		if (StringUtil.isEmpty(path)) {
			path = "/";
		}

		ChannelPipeline cp = ch.pipeline();
		cp.addLast(new HttpRequestDecoder());
		cp.addLast(new HttpObjectAggregator(aggregator));
//		 cp.addLast(new WebSocketFrameAggregator(aggregator));
		cp.addLast(new HttpResponseEncoder());
		cp.addLast(new WebSocketServerProtocolHandler(path));
		cp.addLast(new WebsocketServerHandler(parent));

	}

}
