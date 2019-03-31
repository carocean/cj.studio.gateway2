package cj.studio.gateway.mic.cmd.ct;

import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.ICluster;
import cj.studio.gateway.IConfiguration;
import cj.studio.gateway.mic.IMicConsoleSession;
import cj.studio.gateway.mic.ISendResponse;
import cj.studio.gateway.mic.MicCommand;

public class ValidDestinationMicCommand extends MicCommand {

	@Override
	public String cmd() {
		return "vd";
	}

	@Override
	public String cmdDesc() {
		return "使目标有效，例：vd domain -u tcp://ip:port";
	}

	@Override
	public Options options() {
		Options options = new Options();
//		Option c = new Option("c", "cause", true, "必选,无效原因");
//		options.addOption(c);
		Option c2 = new Option("u", "uri", true, "使目标中指定的uri无效，uri是连接到远程主机地址，形如：tcp://ip:port");
		options.addOption(c2);
		// Option p = OptionBuilder.withArgName("property=value").hasArgs(2)
		// .withValueSeparator()
		// .withDescription("设置服务器相关属性,格式为：-Pproperty=value
		// 如心跳间隔：-PheartbeatInterval=1000").create("P");
		// options.addOption(p);
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
		IConfiguration config = (IConfiguration) session.provider().getService("$.config");
		ICluster cluster = config.getCluster();
		if (!cluster.containsValid(domain) && !cluster.containsInvalid(domain)) {
			sb.append(String.format("<li>%s错误：域不存在：%s</li>", indent, domain));
			sb.append("</ul>");
			response.send(user, sb.toString());
			return;
		}
		if (line.hasOption("u")) {
			try {
				String uri = line.getOptionValue("u");
				cluster.valid(domain, uri);
				config.flushCluster();
				sb.append("</ul>");
				response.send(user, sb.toString());
				return;
			} catch (Exception e) {
				sb.append(String.format("<li><span>%s</span></li>", e));
				sb.append("</ul>");
				response.send(user, sb.toString());
			}

		}
		try {
			cluster.validDestination(domain);
			config.flushCluster();
			sb.append("</ul>");
			response.send(user, sb.toString());
		} catch (Exception e) {
			sb.append(String.format("<li><span>%s</span></li>", e));
			sb.append("</ul>");
			response.send(user, sb.toString());
		}
	}

}
