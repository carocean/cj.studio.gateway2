package cj.studio.gateway;

import java.util.Collection;

import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.socket.Destination;

public interface ICluster {



	boolean containsInvalid(String domain);

	Collection<Destination> getDestinations();

	boolean containsValid(String domain);

	void addDestination(Destination dest);

	void removeDestination(String domain);

	String toJson();

	Destination getDestination(String domain);

	Collection<Destination> listInvalids();

	void invalid(String domain, String invalid_uri, String cause);

	void invalidDestination(String domain, String cause);

	void invalid(String domain, String invalid_uri, CircuitException cause);

	void valid(String domain, String invalid_uri);

	void validDestination(String domain);

}
