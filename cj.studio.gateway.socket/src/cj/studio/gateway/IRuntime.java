package cj.studio.gateway;

import cj.studio.gateway.socket.Destination;

public interface IRuntime {
	void addDestination(Destination dest);
	void removeDestination(String domain);
	Destination getDestination(String domain);
	boolean containsValid(String domain);
	void validDestination(String domain);
	boolean containsInvalid(String domain);
	void invalidDestination(String domain, String cause);
	void flushCluster();
}
