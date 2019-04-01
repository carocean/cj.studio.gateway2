package cj.studio.gateway.mic.cmd.ct;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.ICluster;
import cj.studio.gateway.IConfiguration;
import cj.studio.gateway.mic.IMicConsoleSession;
import cj.studio.gateway.mic.ISendResponse;
import cj.studio.gateway.mic.MicCommand;
import cj.studio.gateway.socket.Destination;
import cj.ultimate.util.StringUtil;

public class AddDestinationMicCommand extends MicCommand {

	@Override
	public String cmd() {
		return "add";
	}

	@Override
	public String cmdDesc() {
		return "添加群族的远程目标，格式：add domain -h xxx 例：add yanxin -h protocol://ip:port,protocol://ip:port。\r\n"
				+ "注意：域名在同一网关中唯一。" + "\r\n支持的协议有：tcp,udt,http,ws,app。连接参数在地址后面加查询串?heartbeat=10\r\n"
				+ "app协议的uri格式是：app://程序相对目录名:适配类型,例：app://wigo:way,适配类型有：jee,way";
	}

	@Override
	public Options options() {
		Options options = new Options();
		Option c = new Option("h", "hosts", true, "主机地址（protocol://ip:port)，如果有多个则以,号分隔");
		options.addOption(c);
//		Option at = new Option("ar", "autorun", false, "自动运行，在网关启动时");
//		options.addOption(at);
		@SuppressWarnings("static-access")
		Option p = OptionBuilder.withArgName("property=value").hasArgs(2).withValueSeparator()
				.withDescription("设置服务器相关属性.").create("P");
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
			sb.append(String.format("<li>%s错误：未指定逻辑域名</li>", indent));
			sb.append("</ul>");
			response.send(user, sb.toString());
			return;
		}
		String domain = args.get(0);
		Destination item = new Destination(domain);

		if (!line.hasOption("h")) {
			sb.append(String.format("<li>%s错误：缺少参数h。</li>", indent));
			sb.append("</ul>");
			response.send(user, sb.toString());
			return;
		}
		if (line.hasOption("ar")) {
			item.getProps().put("autorun", "true");
		}
		String host = line.getOptionValue("h");
		String[] harr = host.split(",");

		for (String h : harr) {
			if (StringUtil.isEmpty(h))
				continue;
			item.getUris().add(h);
		}
		Map<String, String> props = item.getProps();
		if (line.hasOption("P")) {
			Properties list = line.getOptionProperties("P");
			for (Object k : list.keySet()) {
				props.put((String) k, list.getProperty((String) k));
			}
		}
		try {
			ICluster cluster = (ICluster) session.provider().getService("$.cluster");
			cluster.addDestination(item);
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
