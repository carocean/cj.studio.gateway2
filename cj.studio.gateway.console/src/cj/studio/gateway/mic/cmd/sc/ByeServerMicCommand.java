package cj.studio.gateway.mic.cmd.sc;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.mic.IMicConsoleSession;
import cj.studio.gateway.mic.ISendResponse;
import cj.studio.gateway.mic.MicCommand;

public class ByeServerMicCommand extends MicCommand {

	@Override
	public String cmd() {
		return "bye";
	}

	@Override
	public String cmdDesc() {
		return "退出本命令窗口";
	}

	@Override
	public Options options() {
		Options options = new Options();
		return options;
	}

	@Override
	public void doCommand(CommandLine line, String user, ISendResponse response,Frame frame, IMicConsoleSession session)
			throws CircuitException {
		session.bye(user);
	}

}
