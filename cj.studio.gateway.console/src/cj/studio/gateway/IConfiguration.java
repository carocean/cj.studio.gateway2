package cj.studio.gateway;

import java.util.Set;

import cj.studio.gateway.conf.ServerInfo;
import cj.studio.gateway.mic.MicRegistry;

public interface IConfiguration {
	void load();
	void flushServers();
	
	String home();
	Set<String> enumServerNames();
	ServerInfo serverInfo(String name);
	boolean containsServiceName(String name);
	void addServerInfo(ServerInfo item);
	void removeServerInfo(String name);
	void flushCluster();
	ICluster getCluster();
	MicRegistry registry();
	void flushRegistry();
}
