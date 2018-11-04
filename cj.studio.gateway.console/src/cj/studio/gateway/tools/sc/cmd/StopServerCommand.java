package cj.studio.gateway.tools.sc.cmd;

import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceInvertInjection;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.gateway.IGateway;
import cj.studio.gateway.IGatewayServerContainer;
import cj.studio.gateway.tools.CmdLine;
import cj.studio.gateway.tools.Command;
import cj.studio.gateway.tools.Console;

@CjService(name = "stopServerCommand")
public class StopServerCommand extends Command {
	@CjServiceInvertInjection
	@CjServiceRef(refByName = "serversConsole")
	Console console;
	

	public void doCommand(CmdLine cl) throws IOException {
		IGateway gateway = (IGateway) cl.prop("gateway");
		CommandLine line = cl.line();
		@SuppressWarnings("unchecked")
		List<String> args = line.getArgList();
		String indent = (String) cl.prop("indent");
		if (args.isEmpty()) {
			System.out.println(String.format("%s错误：未指定服务器名", indent));
			return;
		}
		String name = args.get(0);
		IGatewayServerContainer container=(IGatewayServerContainer)gateway.getService("$.container.server");
		container.stopServer(name);
	}

	@Override
	public String cmd() {
		return "stop";
	}

	@Override
	public String cmdDesc() {
		return "停止服务器.例：stop serverName";
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
