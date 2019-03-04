package cj.studio.gateway.socket.serverchannel.tcp;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.socket.Destination;
import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.pipeline.IInputPipelineBuilder;
import cj.studio.gateway.socket.serverchannel.AbstractServerChannelSocket;
import cj.studio.gateway.socket.serverchannel.tcp.pipeline.builder.TcpServerChannelInputPipelineBuilder;
import cj.studio.gateway.socket.util.SocketName;
import io.netty.channel.Channel;

public class TcpServerChannelGatewaySocket extends AbstractServerChannelSocket implements IGatewaySocket {
	private IServiceProvider parent;
	private Channel channel;
	private IInputPipelineBuilder builder;
	String gatewayDest;
	public TcpServerChannelGatewaySocket(IServiceProvider parent,String gatewayDest, Channel channel) {
		this.parent = parent;
		this.channel = channel;
		this.gatewayDest=gatewayDest;
		this.builder = new TcpServerChannelInputPipelineBuilder(parent,channel);
	}

	@Override
	public Object getService(String name) {
		if ("$.pipeline.input.builder".equals(name)) {
			return builder;
		}
		if ("$.localAddress".equals(name)) {
			return channel.localAddress().toString();
		}
		if ("$.remoteAddress".equals(name)) {
			return channel.remoteAddress().toString();
		}
		return parent.getService(name);
	}

	@Override
	public <T> ServiceCollection<T> getServices(Class<T> clazz) {
		return parent.getServices(clazz);
	}

	@Override
	public String name() {
//		String netName = (String) parent.getService("$.server.name");
		return SocketName.name(channel.id(), gatewayDest);
	}

	@Override
	public void connect(Destination dest) throws CircuitException {
		throw new CircuitException("503", "不支持该方法");
	}

	@Override
	public void close() throws CircuitException {
		if (channel.isOpen()) {
			channel.close();
		}
	}

}
