package cj.studio.gateway;

import java.util.Set;

import cj.studio.gateway.conf.ServerInfo;

public interface IGatewayServerContainer {
	void startAll();

	void startServer(ServerInfo item);

	void stopServer(String name);

	void stopAll();
	IGatewayServer server(String name);
	Set<String> enumServerName();
	boolean containsServer(String name);
}
