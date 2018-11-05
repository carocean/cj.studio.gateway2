package cj.studio.gateway;

import cj.studio.gateway.socket.IGatewaySocket;

public interface IGatewaySocketContainer {

	IGatewaySocket find(String name);
	void add(IGatewaySocket socket);
	void remove(String name);
	String[] enumSocketName();
	boolean isEmpty();
	int count();
	boolean contains(String name);
}
