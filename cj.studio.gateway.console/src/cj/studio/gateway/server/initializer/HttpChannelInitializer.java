package cj.studio.gateway.server.initializer;

import cj.studio.ecm.IServiceProvider;
import cj.studio.gateway.conf.ServerInfo;
import cj.studio.gateway.server.handler.HttpChannelHandler;
import cj.studio.gateway.socket.util.SocketContants;
import cj.ultimate.util.StringUtil;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

public class HttpChannelInitializer extends ChannelInitializer<SocketChannel> {
	IServiceProvider parent;

	public HttpChannelInitializer(IServiceProvider parent) {
		this.parent=parent;
		
	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		ServerInfo info=(ServerInfo)parent.getService("$.server.info");
		String aggregatorstr = info.getProps().get(SocketContants.__http_ws_prop_aggregatorFileLengthLimit);// 最大聚合的内容大小，一般用于限制文件最大大小的上传
		if (StringUtil.isEmpty(aggregatorstr)) {
			aggregatorstr = "10485760";//默认是10M
		}
		int aggregator = Integer.valueOf(aggregatorstr);
		
		
		ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast("codec-http", new HttpServerCodec());
		pipeline.addLast("aggregator", new HttpObjectAggregator(aggregator));
//	        cp.addLast(new WebSocketFrameAggregator(aggregator));
		pipeline.addLast("deflater", new HttpContentCompressor());
		HttpChannelHandler handler = new HttpChannelHandler(parent);
		pipeline.addLast("handler", handler);
	}
}