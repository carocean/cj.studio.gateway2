package cj.studio.gateway.mic.cmd;

import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.IGatewaySocketContainer;
import cj.studio.gateway.IJunctionTable;
import cj.studio.gateway.junction.BackwardJunction;
import cj.studio.gateway.junction.ForwardJunction;
import cj.studio.gateway.mic.IMicConsoleSession;
import cj.studio.gateway.mic.ISendResponse;
import cj.studio.gateway.mic.MicCommand;
import cj.studio.gateway.socket.IGatewaySocket;
import cj.ultimate.util.StringUtil;

public class CloseSocketMicCommand extends MicCommand {

	@Override
	public String cmd() {
		return "close";
	}

	@Override
	public String cmdDesc() {
		return "关闭socket，如：close website，查看套节字请用命令：ls -s";
	}

	@Override
	public Options options() {
		return new Options();
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
		String name = args.get(0);
		if (StringUtil.isEmpty(name)) {
			sb.append(String.format("<li>%s错误：未指定套节字名</li>", indent));
			sb.append("</ul>");
			response.send(user, sb.toString());
			return;
		}
		IGatewaySocketContainer container = (IGatewaySocketContainer) session.provider()
				.getService("$.container.socket");
		IGatewaySocket socket = container.find(name);
		if (socket == null) {
			sb.append(String.format("<li>%s错误：socket:%s 不存在</li>", indent, name));
			sb.append("</ul>");
			response.send(user, sb.toString());
			return;
		}
		try {
			socket.close();
			IJunctionTable junctionTable = (IJunctionTable) session.provider().getService("$.junctions");
			String[] bnames = junctionTable.enumBackwardName();
			for (String bn : bnames) {
				BackwardJunction jun = (BackwardJunction) junctionTable.findInBackwards(bn);
				if (jun == null)
					continue;
				if (name.equals(jun.getFromWho()) || name.equals(jun.getToWho())) {
					junctionTable.remove(jun);
				}
			}
			String[] fnames = junctionTable.enumForwardName();
			for (String fn : fnames) {
				ForwardJunction jun = (ForwardJunction) junctionTable.findInForwards(fn);
				if (jun == null)
					continue;
				if (name.equals(jun.getToWho())||name.equals(jun.getFromWho())) {
					junctionTable.remove(jun);
				}
			}
		} catch (Exception e) {
			sb.append(String.format("<li>%s%s</li>", indent, e));
			sb.append("</ul>");
			response.send(user, sb.toString());
			throw e;
		}
	}

}
