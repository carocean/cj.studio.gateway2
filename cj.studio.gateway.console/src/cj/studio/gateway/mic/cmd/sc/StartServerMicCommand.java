package cj.studio.gateway.mic.cmd.sc;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.IGatewayServerContainer;
import cj.studio.gateway.ISupportProtocol;
import cj.studio.gateway.conf.ServerInfo;
import cj.studio.gateway.mic.IMicConsoleSession;
import cj.studio.gateway.mic.ISendResponse;
import cj.studio.gateway.mic.MicCommand;

public class StartServerMicCommand extends MicCommand {

	@Override
	public String cmd() {
		return "start";
	}

	@Override
	public String cmdDesc() {
		return "启动服务器，格式：start serverName -h xxx 例：start httpServer -h ip:port";
	}

	@Override
	public Options options() {
		Options options = new Options();
		Option c = new Option("h", "host", true, "主机地址（ip:port)");
		options.addOption(c);
		Option t = new Option("t", "protocol", true, "网络协议。目前支持ws,http,tcp,udt");
		options.addOption(t);
		Option r = new Option("r", "road", true, "过路器。用于将客端信息经过网关转发给远程目标");
		options.addOption(r);
		@SuppressWarnings("static-access")
		Option p = OptionBuilder.withArgName("property=value").hasArgs(2).withValueSeparator()
				.withDescription("设置服务器相关属性,格式为：-Pproperty=value 如心跳间隔（单位秒）：-Pheartbeat=10;\r\n").create("P");
		options.addOption(p);
		return options;
	}

	@Override
	public void doCommand(CommandLine line, String user, ISendResponse response,Frame frame, IMicConsoleSession session)
			throws CircuitException {
		@SuppressWarnings("unchecked")
		List<String> args = line.getArgList();
		String indent = "&nbsp;";
		StringBuilder sb = new StringBuilder();
		sb.append("<ul>");
		if (args.isEmpty()) {
			sb.append(String.format("<li style='padding-left:5px;'>%s错误：未指定服务器名</li>", indent));
			sb.append("</ul>");
			response.send(user, sb.toString());
			return;
		}
		String name = args.get(0);
		ServerInfo item = new ServerInfo(name);
		if (!line.hasOption("t")) {
			sb.append(String.format("<li style='padding-left:5px;'>%s错误：缺少参数t。</li>", indent));
			sb.append("</ul>");
			response.send(user, sb.toString());
			return;
		}
		item.setProtocol(line.getOptionValue("t"));
		if (!line.hasOption("h")) {
			sb.append(String.format("<li style='padding-left:5px;'>%s错误：缺少参数h。</li>", indent));
			sb.append("</ul>");
			response.send(user, sb.toString());
			return;
		}
		String host = line.getOptionValue("h");
		item.setHost(host);
		if (line.hasOption("r")) {
			item.setRoad(line.getOptionValue("r"));
		}
		Map<String, String> props = item.getProps();
		if (line.hasOption("P")) {
			Properties list = line.getOptionProperties("P");
			for (Object k : list.keySet()) {
				props.put((String) k, list.getProperty((String) k));
			}
		}
		ISupportProtocol supportProtocol=(ISupportProtocol)session.provider().getService("$.supportProtocol");
		if (!supportProtocol.supportProtocol(item.getProtocol())) {
			sb.append(
					String.format("<li style='padding-left:5px;'>%s错误：不支持的网络协议：%s。</li>", indent, item.getProtocol()));
			sb.append("</ul>");
			response.send(user, sb.toString());
			return;
		}
		IGatewayServerContainer container = (IGatewayServerContainer) session.provider()
				.getService("$.container.server");
		try {
			container.startServer(item);
			sb.append("</ul>");
			response.send(user, sb.toString());
		} catch (Exception e) {
			sb.append(String.format("<li><span>%s</span></li>", e));
			sb.append("</ul>");
			response.send(user, sb.toString());
		}
	}

}
