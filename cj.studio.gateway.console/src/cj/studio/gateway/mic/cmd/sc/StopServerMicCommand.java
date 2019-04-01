package cj.studio.gateway.mic.cmd.sc;

import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.IGatewayServerContainer;
import cj.studio.gateway.mic.IMicConsoleSession;
import cj.studio.gateway.mic.ISendResponse;
import cj.studio.gateway.mic.MicCommand;

public class StopServerMicCommand extends MicCommand {

	@Override
	public String cmd() {
		return "stop";
	}

	@Override
	public String cmdDesc() {
		return "停止服务器.例：stop serverName";
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
		if (args.isEmpty()) {
			sb.append(String.format("<li>%s错误：未指定服务器名</li>", indent));
			sb.append("</ul>");
			response.send(user, sb.toString());
			return;
		}
		String name = args.get(0);
		IGatewayServerContainer container = (IGatewayServerContainer) session.provider()
				.getService("$.container.server");
		try {
			container.stopServer(name);
			sb.append("</ul>");
			response.send(user, sb.toString());
		} catch (Exception e) {
			sb.append("</ul>");
			sb.append(String.format("<li><span>%s</span></li>", e));
			response.send(user, sb.toString());
			throw e;
		}

	}

}
