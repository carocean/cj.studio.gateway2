package cj.studio.gateway;

import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.Destination;
import cj.studio.gateway.socket.IGatewaySocket;

public interface IDestinationLoader {

	IGatewaySocket load(Destination destination)throws CircuitException;

}
