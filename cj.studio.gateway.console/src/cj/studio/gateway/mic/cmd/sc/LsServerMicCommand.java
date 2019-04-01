package cj.studio.gateway.mic.cmd.sc;

import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.IConfiguration;
import cj.studio.gateway.IGatewayServer;
import cj.studio.gateway.IGatewayServerContainer;
import cj.studio.gateway.conf.ServerInfo;
import cj.studio.gateway.mic.IMicConsoleSession;
import cj.studio.gateway.mic.ISendResponse;
import cj.studio.gateway.mic.MicCommand;
import cj.ultimate.util.StringUtil;

public class LsServerMicCommand extends MicCommand {

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
		return options;
	}

	@Override
	public void doCommand(CommandLine line, String user, ISendResponse response, Frame frame,IMicConsoleSession session)
			throws CircuitException {
		IConfiguration config = (IConfiguration) session.provider().getService("$.config");
		Set<String> names = config.enumServerNames();
		IGatewayServerContainer sc = (IGatewayServerContainer) session.provider().getService("$.container.server");
		String indent = "&nbsp;";
		StringBuilder sb = new StringBuilder();
		sb.append("<ul>");
		for (String name : names) {
			ServerInfo item = config.serverInfo(name);
			sb.append(String.format("<li style='padding-left:5px;'>%snet名：%s</li>", indent, item.getName()));
			sb.append(String.format("<li style='padding-left:40px;'>%s\t协议：%s</li>", indent, item.getProtocol()));
			sb.append(String.format("<li style='padding-left:40px;'>%s\t主机：%s</li>", indent, item.getHost()));
			if (!StringUtil.isEmpty(item.getRoad())) {
				sb.append(String.format("<li style='padding-left:40px;'>%s\t选路：%s</li>", indent, item.getRoad()));
			}
			sb.append(String.format("<li style='padding-left:40px;'>%s\t网络属性：%s</li>", indent, item.getProps()));
			sb.append(String.format("<li style='padding-left:40px;'>%s\t运行情况：", indent));
			if (!sc.containsServer(item.getName())) {
				sb.append(String.format("<li style='padding-left:40px;'>%s\t\t状态：未启用", indent));
			} else {
				IGatewayServer s = sc.server(item.getName());
				sb.append(String.format("<li style='padding-left:40px;'>%s\t\t状态：%s</li>", indent, s.isStarted() ? "已启动" : "未启动"));
				sb.append(String.format("<li style='padding-left:40px;'>%s\t\t主线程：%s/%s</li>", indent, s.activeBossCount(), s.bossThreadCount()));
				sb.append(String.format("<li style='padding-left:40px;'>%s\t\t工作线程：%s/%s</li>", indent, s.activeWorkCount(), s.workThreadCount()));
			}
			sb.append("<br>");
		}
		sb.append("</ul>");
		response.send(user, sb.toString());
	}

}
