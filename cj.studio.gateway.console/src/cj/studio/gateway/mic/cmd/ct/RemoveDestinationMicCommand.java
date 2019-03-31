package cj.studio.gateway.mic.cmd.ct;

import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.ICluster;
import cj.studio.gateway.IConfiguration;
import cj.studio.gateway.mic.IMicConsoleSession;
import cj.studio.gateway.mic.ISendResponse;
import cj.studio.gateway.mic.MicCommand;

public class RemoveDestinationMicCommand extends MicCommand {

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

	@Override
	public void doCommand(CommandLine line, String user, ISendResponse response, IMicConsoleSession session)
			throws CircuitException {
		@SuppressWarnings("unchecked")
		List<String> args = line.getArgList();
		String indent = "&nbsp;";
		StringBuilder sb = new StringBuilder();
		sb.append("<ul>");
		if (args.isEmpty()) {
			sb.append(String.format("<li>%s错误：未指定逻辑域名</li>", indent));
			sb.append("</ul>");
			response.send(user, sb.toString());
			return;
		}
		String domain = args.get(0);
		try {
			ICluster cluster = (ICluster) session.provider().getService("$.cluster");
			cluster.removeDestination(domain);
			IConfiguration config = (IConfiguration) session.provider().getService("$.config");
			config.flushCluster();
			sb.append("</ul>");
			response.send(user, sb.toString());
		} catch (Exception e) {
			sb.append(String.format("<li><span>%s</span></li>", e));
			sb.append("</ul>");
			response.send(user, sb.toString());
			throw e;
		}
	}

}
