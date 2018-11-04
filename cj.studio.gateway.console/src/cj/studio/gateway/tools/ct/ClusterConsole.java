package cj.studio.gateway.tools.ct;

import java.io.IOException;
import java.util.Map;

import cj.studio.ecm.annotation.CjService;
import cj.studio.gateway.IGateway;
import cj.studio.gateway.tools.Command;
import cj.studio.gateway.tools.Console;

@CjService(name = "clusterConsole")
public class ClusterConsole extends Console  {
	public static final String COLOR_SURFACE = "\033[0;30m";
	public static final String COLOR_RESPONSE = "\033[0;34m";
	public static final String COLOR_CMDLINE = "\033[0;32m";
	public static final String COLOR_CMDPREV = "\033[0;31m";
	@Override
	protected String prefix(IGateway gateway, Object... target) {
		return ClusterConsole.COLOR_CMDPREV +"ct>"
				+ ClusterConsole.COLOR_CMDLINE;
	}
	@Override
	public void monitor(IGateway gateway, Object... target)
			throws IOException {
		System.out.println("——————————————使用说明——————————————");
		System.out.println("       如不记得命令，可用man命令查询");
		System.out.println("__________________________________");
		System.out.println();
		super.monitor(gateway, target);
	}
	@Override
	protected void printMan(IGateway gateway, Object[] target,Map<String, Command> cmds) {
		System.out.println("群簇配置 指令集");
		super.printMan(gateway,target,cmds);
	}
	@Override
	protected boolean exit(String cmd) {
		if ("exit".equals(cmd) || "bye".equals(cmd) || "quit".equals(cmd)
				|| "close".equals(cmd)) {
			return true;
		}
		return false;
	}

}
