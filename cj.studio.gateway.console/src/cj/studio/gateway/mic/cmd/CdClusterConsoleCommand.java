package cj.studio.gateway.mic.cmd;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.mic.IMicConsoleSession;
import cj.studio.gateway.mic.ISendResponse;
import cj.studio.gateway.mic.MicCommand;
import cj.studio.gateway.mic.cmd.ct.ClusterMicConsole;

public class CdClusterConsoleCommand extends MicCommand {

	@Override
	public String cmd() {
		return "ct";
	}

	@Override
	public String cmdDesc() {
		return "进入群簇配置窗口";
	}

	@Override
	public Options options() {
		return new Options();
	}

	@Override
	public void doCommand(CommandLine line, String user, ISendResponse response,Frame frame, IMicConsoleSession session)
			throws CircuitException {
		session.cd(user,new ClusterMicConsole(cmd()));
	}

}
