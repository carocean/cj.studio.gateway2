package cj.studio.gateway.server;

import cj.studio.ecm.IServiceProvider;
import cj.studio.gateway.server.initializer.FileChannelInitializer;
import io.netty.channel.ChannelHandler;
//文件服务器，文件的读写，不论大小文件，非文件被禁止
public class FileGatewayServer extends HttpGatewayServer {

	public FileGatewayServer(IServiceProvider parent) {
		super(parent);
	}
	@Override
	protected ChannelHandler createChannelInitializer() {
		return new FileChannelInitializer();
	}
}
