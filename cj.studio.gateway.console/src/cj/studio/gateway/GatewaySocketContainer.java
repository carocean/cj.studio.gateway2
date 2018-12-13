package cj.studio.gateway;

import java.util.HashMap;
import java.util.Map;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.IServiceSetter;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.socket.Destination;
import cj.studio.gateway.socket.IGatewaySocket;

@CjService(name = "gatewaySocketContainer")
public class GatewaySocketContainer implements IGatewaySocketContainer,IServiceSetter {
	Map<String, IGatewaySocket> sockets;
	IServiceProvider parent;
	public GatewaySocketContainer() {
		this.sockets = new HashMap<>();
	}
	@Override
	public void setService(String arg0, Object obj) {
		parent=(IServiceProvider)obj;
		
	}
	@Override
	public synchronized IGatewaySocket getAndCreate(String name) throws CircuitException {
		IGatewaySocket socket = sockets.get(name);
		if (socket != null)
			return socket;
		ICluster cluster = (ICluster) parent.getService("$.cluster");
		Destination destination = cluster.getDestination(name);
		if (destination == null) {
			throw new CircuitException("404", "簇中缺少目标:" + name);
		}
		IDestinationLoader loader = (IDestinationLoader) parent.getService("$.dloader");
		socket = loader.load(destination);
		this.add(socket);
		return socket;
	}

	@Override
	public IGatewaySocket find(String name) {
		return sockets.get(name);
	}

	@Override
	public boolean contains(String name) {
		// TODO Auto-generated method stub
		return sockets.containsKey(name);
	}

	@Override
	public void add(IGatewaySocket socket) {
		sockets.put(socket.name(), socket);
	}

	@Override
	public void remove(String name) {
		sockets.remove(name);

	}

	@Override
	public String[] enumSocketName() {
		return sockets.keySet().toArray(new String[0]);
	}

	@Override
	public boolean isEmpty() {
		return sockets.isEmpty();
	}

	@Override
	public int count() {
		return sockets.size();
	}

}
