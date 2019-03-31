package cj.studio.gateway.tools.ct.cmd;

import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceInvertInjection;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.gateway.ICluster;
import cj.studio.gateway.IConfiguration;
import cj.studio.gateway.IGateway;
import cj.studio.gateway.tools.CmdLine;
import cj.studio.gateway.tools.Command;
import cj.studio.gateway.tools.Console;

@CjService(name = "removeDestinationCommand")
public class RemoveDestinationCommand extends Command {
	@CjServiceInvertInjection
	@CjServiceRef(refByName = "clusterConsole")
	Console console;
	

	public void doCommand(CmdLine cl) throws IOException {
		IGateway gateway = (IGateway) cl.prop("gateway");
		CommandLine line = cl.line();
		@SuppressWarnings("unchecked")
		List<String> args = line.getArgList();
		String indent = (String) cl.prop("indent");
		if (args.isEmpty()) {
			System.out.println(String.format("%s错误：未指定逻辑域名", indent));
			return;
		}
		String domain = args.get(0);
		ICluster cluster =(ICluster)gateway.getService("$.cluster");
		cluster.removeDestination(domain);
		IConfiguration config =(IConfiguration)gateway.getService("$.config");
		config.flushCluster();
	}

	@Override
	public String cmd() {
		return "remove";
	}

	@Override
	public String cmdDesc() {
		return "移除远程目标。格式：remove domain";
	}

	@Override
	public Options options() {
		Options options = new Options();
//		Option c = new Option("h", "hosts", true, "主机地址（ip:port)，如果有多个则以,号分隔");
//		options.addOption(c);
//		Option p = OptionBuilder.withArgName("property=value").hasArgs(2)
//				.withValueSeparator()
//				.withDescription("设置服务器相关属性,格式为：-Pproperty=value 如心跳间隔：-PheartbeatInterval=1000").create("P");
//		options.addOption(p);
		return options;
	}
}
