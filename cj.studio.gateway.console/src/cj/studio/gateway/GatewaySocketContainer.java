package cj.studio.gateway;

import java.util.HashMap;
import java.util.Map;

import cj.studio.ecm.annotation.CjService;
import cj.studio.gateway.socket.IGatewaySocket;

@CjService(name="gatewaySocketContainer")
public class GatewaySocketContainer implements IGatewaySocketContainer{
	Map<String, IGatewaySocket> sockets;
	public GatewaySocketContainer() {
		this.sockets=new HashMap<>();
	}
	@Override
	public IGatewaySocket find(String name) {
		return sockets.get(name);
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
