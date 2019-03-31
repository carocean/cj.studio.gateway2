package cj.studio.gateway.mic.cmd;

import java.util.List;

import cj.studio.gateway.mic.IMicConsole;
import cj.studio.gateway.mic.MicCommand;
import cj.studio.gateway.mic.MicConsole;

public class MainConsole extends MicConsole implements IMicConsole {
	public MainConsole() {
		super("$");
	}
	@Override
	protected void register(List<MicCommand> registry) {
		registry.add(new LsCommand());
		registry.add(new ResetCommand());
		registry.add(new CdServerConsoleCommand());
		registry.add(new CdClusterConsoleCommand());
	}

	@Override
	protected String manHeader() {
		return "主窗命令集";
	}

}
