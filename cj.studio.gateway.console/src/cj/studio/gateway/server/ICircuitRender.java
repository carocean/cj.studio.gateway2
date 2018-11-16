package cj.studio.gateway.server;

import cj.studio.ecm.frame.Circuit;
import io.netty.channel.ChannelHandlerContext;

public interface ICircuitRender {
	void accept(Circuit circuit);
	void render(ChannelHandlerContext ctx, Object request);

}
