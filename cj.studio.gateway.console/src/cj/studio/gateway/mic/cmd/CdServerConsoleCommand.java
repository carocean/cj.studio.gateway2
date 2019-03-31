package cj.studio.gateway.mic.cmd;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.mic.IMicConsoleSession;
import cj.studio.gateway.mic.ISendResponse;
import cj.studio.gateway.mic.MicCommand;
import cj.studio.gateway.mic.cmd.sc.ServerMicConsole;

public class CdServerConsoleCommand extends MicCommand {

	@Override
	public String cmd() {
		return "sc";
	}

	@Override
	public String cmdDesc() {
		return "进入服务器配置窗口";
	}

	@Override
	public Options options() {
		return new Options();
	}

	@Override
	public void doCommand(CommandLine line, String user, ISendResponse response, IMicConsoleSession session)
			throws CircuitException {
		session.cd(user, new ServerMicConsole(cmd()));
	}

}
