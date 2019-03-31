package cj.studio.gateway.mic;

import cj.studio.ecm.net.CircuitException;

public interface ISendResponse {
	void send(String user, String response) throws CircuitException;

	void onCDConsole(String user, String consoleName) throws CircuitException;
	void onByeConsole(String user,String consoleName)throws CircuitException;
}
