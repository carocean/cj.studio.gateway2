package cj.studio.gateway.mic.cmd.ct;

import java.io.File;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.ICluster;
import cj.studio.gateway.mic.IMicConsoleSession;
import cj.studio.gateway.mic.ISendResponse;
import cj.studio.gateway.mic.MicCommand;
import cj.ultimate.util.FileHelper;
import cj.ultimate.util.StringUtil;

public class RmMicCommand extends MicCommand {

	@Override
	public String cmd() {
		return "rm";
	}

	@Override
	public String cmdDesc() {
		return "删除指定应用目标目录、或其中的主程序、插件。例：删除主程序集 rm website -m jar包名,删除插件：rm website -p 插件目录名,删除整个程序根目录：rm website -a";
	}

	@Override
	public Options options() {
		Options options = new Options();
		Option c = new Option("m", "main", true, "应用根目录下的程序包");
		options.addOption(c);
		Option at = new Option("p", "plugin", true, "插件目录名");
		options.addOption(at);
		Option a = new Option("a", "all", false, "删除整个应用程序根目录");
		options.addOption(a);
		return options;
	}

	@Override
	public void doCommand(CommandLine line, String user, ISendResponse response, Frame frame,
			IMicConsoleSession session) throws CircuitException {
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
		if (StringUtil.isEmpty(domain)) {
			sb.append("<li><span>缺少目标名</span></li>");
			sb.append("</ul>");
			response.send(user, sb.toString());
			return;
		}
		ICluster cluster = (ICluster) session.provider().getService("$.cluster");
		if (!cluster.containsValid(domain)) {
			sb.append(String.format("<li><span>目标:%s 不存在</span></li>", domain));
			sb.append("</ul>");
			response.send(user, sb.toString());
			return;
		}
		String gatewayHome = (String) session.provider().getService("$.homeDir");
		String appHome = String.format("%s%sassemblies%s%s%s", gatewayHome, File.separator, File.separator, domain,
				File.separator);
		if (line.hasOption("a")) {
			FileHelper.deleteDir(new File(appHome));
			sb.append(String.format("<li><span>应用目录:%s 已被删除</span></li>", domain));
			sb.append("</ul>");
			response.send(user, sb.toString());
			return;
		}
		if (line.hasOption("m")) {
			String m = line.getOptionValue("m");
			String file = String.format("%s%s", appHome, m);
			File f = new File(file);
			if (f.isDirectory()) {
				FileHelper.deleteDir(f);
			} else {
				f.delete();
			}
			sb.append(String.format("<li><span>程序:%s 已被删除</span></li>", m));
		}
		if (line.hasOption("p")) {
			String p = line.getOptionValue("p");
			String file = String.format("%splugins%s%s", appHome, File.separator, p);
			FileHelper.deleteDir(new File(file));
			sb.append(String.format("<li><span>插件:%s 已被删除</span></li>", p));
		}
		sb.append("<li>命令执行完毕</li>");
		sb.append("</ul>");
		response.send(user, sb.toString());
	}

}
