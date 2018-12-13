package cj.studio.gateway;

import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.socket.IGatewaySocket;

public interface IGatewaySocketContainer {
	IGatewaySocket getAndCreate(String name) throws CircuitException;
	IGatewaySocket find(String name);
	void add(IGatewaySocket socket);
	void remove(String name);
	String[] enumSocketName();
	boolean isEmpty();
	int count();
	boolean contains(String name);
}
