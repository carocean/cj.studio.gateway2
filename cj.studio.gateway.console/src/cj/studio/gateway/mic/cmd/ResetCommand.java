package cj.studio.gateway.mic.cmd;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.mic.IMicConsoleSession;
import cj.studio.gateway.mic.ISendResponse;
import cj.studio.gateway.mic.MicCommand;

public class ResetCommand extends MicCommand {

	@Override
	public String cmd() {
		return "reset";
	}

	@Override
	public String cmdDesc() {
		// TODO Auto-generated method stub
		return "重置客户端命令行窗口";
	}

	@Override
	public Options options() {
		return new Options();
	}

	@Override
	public void doCommand(CommandLine line, String user, ISendResponse response,Frame frame,IMicConsoleSession session)
			throws CircuitException {
		
	}

}
