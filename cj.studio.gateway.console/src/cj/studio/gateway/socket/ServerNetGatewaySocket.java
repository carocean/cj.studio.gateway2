package cj.studio.gateway.socket;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.Destination;
import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.util.SocketName;
import io.netty.channel.Channel;

public class ServerNetGatewaySocket implements IGatewaySocket{
	private IServiceProvider parent;
	private Channel channel;
	public ServerNetGatewaySocket(IServiceProvider parent,Channel channel) {
		this.parent=parent;
		this.channel=channel;
	}

	@Override
	public Object getService(String name) {
		return parent.getService(name);
	}

	@Override
	public <T> ServiceCollection<T> getServices(Class<T> clazz) {
		return parent.getServices(clazz);
	}

	@Override
	public String name() {
		String netName=(String)parent.getService("$.server.name");
		return SocketName.name(channel.id(),netName);
	}

	@Override
	public void connect(Destination dest) throws CircuitException {
		throw new CircuitException("503", "不支持该方法");
	}

	@Override
	public void close() throws CircuitException {
		channel.close();
	}


}
