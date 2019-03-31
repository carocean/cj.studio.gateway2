package cj.studio.gateway.mic;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.net.CircuitException;

public interface IMicConsoleSession {

	IMicConsole current(String user)throws CircuitException;

	void cd(String user, IMicConsole console) throws CircuitException;

	IServiceProvider provider();

	void bye(String user) throws CircuitException;

}
