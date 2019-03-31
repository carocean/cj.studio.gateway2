package cj.studio.gateway.mic.cmd.sc;

import java.util.List;

import cj.studio.gateway.mic.IMicConsole;
import cj.studio.gateway.mic.MicCommand;
import cj.studio.gateway.mic.MicConsole;

public class ServerMicConsole extends MicConsole implements IMicConsole {
	public ServerMicConsole(String name) {
		super(name);
	}
	@Override
	protected String manHeader() {
		return "服务器命令集";
	}

	@Override
	protected void register(List<MicCommand> registry) {
		registry.add(new LsServerMicCommand());
		registry.add(new StopServerMicCommand());
		registry.add(new StartServerMicCommand());
		registry.add(new SetSSLServerMicCommand());
		registry.add(new ByeServerMicCommand());
	}

}
