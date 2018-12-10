package cj.studio.gateway.tools.sc.cmd;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceInvertInjection;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.gateway.IGateway;
import cj.studio.gateway.IGatewayServerContainer;
import cj.studio.gateway.conf.ServerInfo;
import cj.studio.gateway.tools.CmdLine;
import cj.studio.gateway.tools.Command;
import cj.studio.gateway.tools.Console;

@CjService(name = "startServerCommand")
public class StartServerCommand extends Command {
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
		ServerInfo item = new ServerInfo(name);
		if (!line.hasOption("t")) {
			System.out.println(String.format("%s错误：缺少参数t。", indent));
			return;
		}
		item.setProtocol(line.getOptionValue("t"));
		if (!line.hasOption("h")) {
			System.out.println(String.format("%s错误：缺少参数h。", indent));
			return;
		}
		String host = line.getOptionValue("h");
		item.setHost(host);
		if(line.hasOption("r")) {
			item.setRoad(line.getOptionValue("r"));
		}
		Map<String, String> props = item.getProps();
		if (line.hasOption("P")) {
			Properties list = line.getOptionProperties("P");
			for (Object k : list.keySet()) {
				props.put((String) k, list.getProperty((String) k));
			}
		}
		if (!gateway.supportProtocol(item.getProtocol())) {
			System.out.println(String.format("%s错误：不支持的网络协议：%s。", indent, item.getProtocol()));
			return;
		}
		IGatewayServerContainer container=(IGatewayServerContainer)gateway.getService("$.container.server");
		container.startServer(item);
	}

	@Override
	public String cmd() {
		return "start";
	}

	@Override
	public String cmdDesc() {
		return "启动服务器，格式：start serverName -h xxx 例：start httpServer -h ip:port";
	}

	@SuppressWarnings("static-access")
	@Override
	public Options options() {
		Options options = new Options();
		Option c = new Option("h", "host", true, "主机地址（ip:port)");
		options.addOption(c);
		Option t = new Option("t", "protocol", true, "网络协议。目前支持ws,http,tcp,udt");
		options.addOption(t);
		Option r = new Option("r", "road", true, "过路器。用于将客端信息经过网关转发给远程目标");
		options.addOption(r);
		Option p = OptionBuilder.withArgName("property=value").hasArgs(2).withValueSeparator()
				.withDescription("设置服务器相关属性,格式为：-Pproperty=value 如心跳间隔（单位秒）：-Pheartbeat=10;\r\n").create("P");
		options.addOption(p);
		return options;
	}
}
