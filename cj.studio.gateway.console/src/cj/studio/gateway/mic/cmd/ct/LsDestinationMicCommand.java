package cj.studio.gateway.mic.cmd.ct;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.ICluster;
import cj.studio.gateway.mic.IMicConsoleSession;
import cj.studio.gateway.mic.ISendResponse;
import cj.studio.gateway.mic.MicCommand;
import cj.studio.gateway.socket.Destination;
import cj.ultimate.util.StringUtil;

public class LsDestinationMicCommand extends MicCommand {

	@Override
	public String cmd() {
		return "ls";
	}

	@Override
	public String cmdDesc() {
		return "列出群簇目标，如果要查看指定的目标则用：ls 目标名";
	}

	@Override
	public Options options() {
		Options options = new Options();
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
		ICluster cluster = (ICluster) session.provider().getService("$.cluster");
		if (!args.isEmpty()) {
			String domain = args.get(0);
			if (!StringUtil.isEmpty(domain)) {
				Destination item = cluster.getDestination(domain);
				sb.append(String.format("<li>%s域名：%s</li>", indent, item.getName()));
				sb.append(String.format("<li style='padding-left:30px;'>%s主机表：%s</li>", indent, item.getUris()));
				sb.append(String.format("<li style='padding-left:30px;'>%s网络属性：%s</li>", indent, item.getProps()));
				sb.append(String.format("<li style='padding-left:30px;'>%s应用目录：</li>", indent));
				for (String uri : item.getUris()) {
					if (uri.startsWith("app://")) {
						printAppInfo(item.getName(), uri, sb, user, response, session);
					}
				}
				sb.append("</ul>");
				response.send(user, sb.toString());
				return;
			}
		}

		Collection<Destination> items = cluster.getDestinations();
		sb.append("-----有效----------");
		for (Destination item : items) {
			sb.append(String.format("<li>%s域名：%s</li>", indent, item.getName()));
			sb.append(String.format("<li style='padding-left:30px;'>%s主机表：%s</li>", indent, item.getUris()));
			sb.append(String.format("<li style='padding-left:30px;'>%s网络属性：%s</li>", indent, item.getProps()));
			sb.append(String.format("<li style='padding-left:30px;'>%s应用目录：</li>", indent));
			for (String uri : item.getUris()) {
				if (uri.startsWith("app://")) {
					printAppInfo(item.getName(), uri, sb, user, response, session);
				}
			}
			sb.append("<br>");
		}

		Collection<Destination> invalids = cluster.listInvalids();
		sb.append("-----无效----------");
		for (Destination item : invalids) {
			sb.append(String.format("<li>%s域名：%s</li>", indent, item.getName()));
			sb.append(String.format("<li style='padding-left:30px;'>%s主机表：%s</li>", indent, item.getUris()));
			sb.append(String.format("<li style='padding-left:30px;'>%s网络属性：%s</li>", indent, item.getProps()));
			sb.append("<br>");
		}
		sb.append("</ul>");
		response.send(user, sb.toString());
	}

	private void printAppInfo(String destname, String uri, StringBuilder sb, String user, ISendResponse response,
			IMicConsoleSession session) {
		String appHome = uri.substring("app://".length(), uri.length());
		appHome = appHome.substring(0, appHome.indexOf(":"));
		String gatewayHome = (String) session.provider().getService("$.homeDir");
		String appPath = String.format("%s%sassemblies%s%s", gatewayHome, File.separator, File.separator, appHome);
		File f = new File(appPath);
		if (!f.exists()) {
			return;
		}
		File[] files = f.listFiles();
		for (File fl : files) {
			sb.append(String.format("<li style='padding-left:50px;'>&nbsp;%s %s</li>", fl.isDirectory() ? "-D" : "-F",
					fl.getName()));
			if (fl.getName().equals("plugins")) {
				File pl[] = fl.listFiles();
				for (File item : pl) {
					sb.append(String.format("<li style='padding-left:70px;'>&nbsp;%s %s</li>",
							item.isDirectory() ? "-D" : "-F", item.getName()));
					File[] lf = item.listFiles();
					for (File lfi : lf) {
						sb.append(String.format("<li style='padding-left:90px;'>&nbsp;%s %s</li>",
								lfi.isDirectory() ? "-D" : "-F", lfi.getName()));
					}
				}
			}
		}
	}

}
