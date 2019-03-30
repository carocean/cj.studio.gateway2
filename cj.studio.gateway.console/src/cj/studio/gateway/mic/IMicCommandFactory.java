package cj.studio.gateway.mic;

import cj.studio.ecm.net.CircuitException;

public interface IMicCommandFactory {

	void exeCommand(String cmdline, String channel)throws CircuitException;

}
