package cj.studio.gateway.mic.cmd.ct;

import java.util.Collection;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.ICluster;
import cj.studio.gateway.mic.IMicConsoleSession;
import cj.studio.gateway.mic.ISendResponse;
import cj.studio.gateway.mic.MicCommand;
import cj.studio.gateway.socket.Destination;

public class LsDestinationMicCommand extends MicCommand {

	@Override
	public String cmd() {
		return "ls";
	}

	@Override
	public String cmdDesc() {
		return "列出群簇目标";
	}

	@Override
	public Options options() {
		Options options = new Options();
		return options;
	}

	@Override
	public void doCommand(CommandLine line, String user, ISendResponse response, IMicConsoleSession session)
			throws CircuitException {
		String indent = "&nbsp;";
		StringBuilder sb=new StringBuilder();
		sb.append("<ul>");
		ICluster cluster=(ICluster)session.provider().getService("$.cluster");
		Collection<Destination> items = cluster.getDestinations();
		sb.append("-----有效----------");
		for (Destination item : items) {
			sb.append(String.format("<li>%s域名：%s</li>", indent, item.getName()));
			sb.append(String.format("<li style='padding-left:30px;'>%s主机表：%s</li>", indent, item.getUris()));
			sb.append(String.format("<li style='padding-left:30px;'>%s网络属性：%s</li>", indent, item.getProps()));
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

}
