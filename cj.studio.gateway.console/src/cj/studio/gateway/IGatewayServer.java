package cj.studio.gateway;

import cj.studio.ecm.IServiceProvider;
import cj.studio.gateway.conf.ServerInfo;

public interface IGatewayServer extends IServiceProvider{

	void stop();


	void start(ServerInfo si);

	String netName();


	boolean isStarted();


	int bossThreadCount();


	int activeBossCount();


	int workThreadCount();


	int activeWorkCount();
	
}
