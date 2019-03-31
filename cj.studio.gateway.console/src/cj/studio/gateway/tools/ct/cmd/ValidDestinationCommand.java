package cj.studio.gateway.tools.ct.cmd;

import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
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

@CjService(name = "validDestinationCommand")
public class ValidDestinationCommand extends Command {
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
		IConfiguration config =(IConfiguration)gateway.getService("$.config");
		ICluster cluster=config.getCluster();
		if (!cluster.containsValid(domain)&&!cluster.containsInvalid(domain)) {
			System.out.println(String.format("%s错误：域不存在：%s", indent,domain));
			return;
		}
		if (line.hasOption("u")) {
			String uri = line.getOptionValue("u");
			cluster.valid(domain, uri);
			config.flushCluster();
			return;
		}
		cluster.validDestination(domain);
		config.flushCluster();
	}

	@Override
	public String cmd() {
		return "vd";
	}

	@Override
	public String cmdDesc() {
		return "使目标有效，例：vd domain -u tcp://ip:port";
	}

	@SuppressWarnings("static-access")
	@Override
	public Options options() {
		Options options = new Options();
//		Option c = new Option("c", "cause", true, "必选,无效原因");
//		options.addOption(c);
		Option c2 = new Option("u", "uri", true, "可选,使目标中指定的uri无效，uri是连接到远程主机地址，形如：tcp://ip:port");
		options.addOption(c2);
		// Option p = OptionBuilder.withArgName("property=value").hasArgs(2)
		// .withValueSeparator()
		// .withDescription("设置服务器相关属性,格式为：-Pproperty=value
		// 如心跳间隔：-PheartbeatInterval=1000").create("P");
		// options.addOption(p);
		return options;
	}
}
