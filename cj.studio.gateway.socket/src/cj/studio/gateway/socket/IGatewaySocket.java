package cj.studio.gateway.socket;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.net.CircuitException;

public interface IGatewaySocket extends IServiceProvider{

	String name();

	void connect(Destination dest)throws CircuitException;
	void close()throws CircuitException;


}
