package cj.studio.gateway.mic;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import cj.studio.ecm.net.CircuitException;

public abstract class MicCommand {
	public abstract String cmd();
	public abstract String cmdDesc();
	public abstract Options options();
	public abstract void doCommand(CommandLine line, String user, ISendResponse response, IMicConsoleSession session) throws CircuitException;

}
