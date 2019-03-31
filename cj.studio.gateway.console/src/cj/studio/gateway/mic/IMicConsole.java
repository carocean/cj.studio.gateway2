package cj.studio.gateway.mic;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.net.CircuitException;

public interface IMicConsole {

	void printMan() throws CircuitException;

	MicCommand get(String cmdName);

	boolean isInited(String user);

	void init(String user, IServiceProvider parent) throws CircuitException;

	String name();

}
