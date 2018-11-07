package cj.studio.gateway;

import cj.studio.ecm.IServiceProvider;

public interface IGateway extends IServiceProvider {
	void start();
	void stop();
	void setHomeDir(String homeDir);
	boolean supportProtocol(String protocol);
}
