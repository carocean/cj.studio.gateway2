package cj.studio.gateway.tools.sc.cmd;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.cli.Options;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceInvertInjection;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.gateway.IConfiguration;
import cj.studio.gateway.IGateway;
import cj.studio.gateway.IGatewayServer;
import cj.studio.gateway.IGatewayServerContainer;
import cj.studio.gateway.conf.ServerInfo;
import cj.studio.gateway.tools.CmdLine;
import cj.studio.gateway.tools.Command;
import cj.studio.gateway.tools.Console;

@CjService(name = "lsServersCommand")
public class LsServersCommand extends Command {
	@CjServiceInvertInjection
	@CjServiceRef(refByName = "serversConsole")
	Console console;
	

	public void doCommand(CmdLine cl) throws IOException {
		IGateway gateway = (IGateway) cl.prop("gateway");
		String indent = (String) cl.prop("indent");
		IConfiguration config=(IConfiguration)gateway.getService("$.config");
		Set<String> names = config.enumServerNames();
		IGatewayServerContainer sc=(IGatewayServerContainer)gateway.getService("$.container.server");
		System.out.println();
		for (String name : names) {
			ServerInfo item=config.serverInfo(name);
			System.out.println(String.format("%snet名：%s", indent, item.getName()));
			System.out.println(String.format("%s\t协议：%s", indent, item.getProtocol()));
			System.out.println(String.format("%s\t主机：%s", indent, item.getHost()));
			System.out.println(String.format("%s\t网络属性：%s", indent, item.getProps()));
			System.out.println(String.format("%s\t运行情况：", indent));
			if(!sc.containsServer(item.getName())){
				System.out.println(String.format("%s\t\t状态：未启用", indent));
			}else{
				IGatewayServer s=sc.server(item.getName());
				System.out.println(String.format("%s\t\t状态：%s", indent, s.isStarted()?"已启动":"未启动"));
				System.out.println(String.format("%s\t\t主线程：%s/%s", indent, s.activeBossCount(),s.bossThreadCount()));
				System.out.println(String.format("%s\t\t工作线程：%s/%s", indent,s.activeWorkCount(), s.workThreadCount()));
			}
			System.out.println();
		}
	}

	@Override
	public String cmd() {
		return "ls";
	}

	@Override
	public String cmdDesc() {
		return "列出服务器";
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
