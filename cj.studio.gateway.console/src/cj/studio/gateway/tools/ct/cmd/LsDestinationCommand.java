package cj.studio.gateway.tools.ct.cmd;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.cli.Options;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceInvertInjection;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.gateway.ICluster;
import cj.studio.gateway.IGateway;
import cj.studio.gateway.socket.Destination;
import cj.studio.gateway.tools.CmdLine;
import cj.studio.gateway.tools.Command;
import cj.studio.gateway.tools.Console;

@CjService(name = "lsDestinationCommand")
public class LsDestinationCommand extends Command {
	@CjServiceInvertInjection
	@CjServiceRef(refByName = "clusterConsole")
	Console console;
	

	public void doCommand(CmdLine cl) throws IOException {
		IGateway gateway = (IGateway) cl.prop("gateway");
		String indent = (String) cl.prop("indent");
		ICluster cluster=(ICluster)gateway.getService("$.cluster");
		Collection<Destination> items = cluster.getDestinations();
		System.out.println("-----有效----------");
		for (Destination item : items) {
			System.out.println(String.format("%s域名：%s", indent, item.getName()));
			System.out.println(String.format("%s\t主机表：%s", indent, item.getUris()));
			System.out.println(String.format("%s\t网络属性：%s", indent, item.getProps()));
			System.out.println();
		}
		
		Collection<Destination> invalids = cluster.listInvalids();
		System.out.println("-----无效----------");
		for (Destination item : invalids) {
			System.out.println(String.format("%s域名：%s", indent, item.getName()));
			System.out.println(String.format("%s\t主机表：%s", indent, item.getUris()));
			System.out.println(String.format("%s\t网络属性：%s", indent, item.getProps()));
			System.out.println();
		}
	}
	
	

	@Override
	public String cmd() {
		return "ls";
	}

	@Override
	public String cmdDesc() {
		return "列出群簇目标";
	}

	@SuppressWarnings("static-access")
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
