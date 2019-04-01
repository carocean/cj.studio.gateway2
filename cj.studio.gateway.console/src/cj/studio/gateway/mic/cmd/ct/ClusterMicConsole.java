package cj.studio.gateway.mic.cmd.ct;

import java.util.List;

import cj.studio.gateway.mic.IMicConsole;
import cj.studio.gateway.mic.MicCommand;
import cj.studio.gateway.mic.MicConsole;

public class ClusterMicConsole extends MicConsole implements IMicConsole {
	public ClusterMicConsole(String name) {
		super(name);
	}
	@Override
	protected String manHeader() {
		return "群簇命令集";
	}

	@Override
	protected void register(List<MicCommand> registry) {
		registry.add(new LsDestinationMicCommand());
		registry.add(new ByeClusterMicCommand());
		registry.add(new AddDestinationMicCommand());
		registry.add(new InvalidDestinationMicCommand());
		registry.add(new ValidDestinationMicCommand());
		registry.add(new RemoveDestinationMicCommand());
		registry.add(new InstallAppMicCommand());
		registry.add(new PluginAppMicCommand());
		registry.add(new RmMicCommand());
	}

}
