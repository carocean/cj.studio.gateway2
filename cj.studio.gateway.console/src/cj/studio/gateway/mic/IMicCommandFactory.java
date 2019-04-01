package cj.studio.gateway.mic;

import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;

public interface IMicCommandFactory {

	void exeCommand(String cmdline,String user,Frame frame)throws CircuitException;

}
