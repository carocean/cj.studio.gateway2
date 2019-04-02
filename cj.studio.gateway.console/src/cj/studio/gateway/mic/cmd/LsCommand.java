package cj.studio.gateway.mic.cmd;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.IGatewaySocketContainer;
import cj.studio.gateway.IJunctionTable;
import cj.studio.gateway.junction.BackwardJunction;
import cj.studio.gateway.junction.ForwardJunction;
import cj.studio.gateway.junction.IJunctionListener;
import cj.studio.gateway.junction.Junction;
import cj.studio.gateway.mic.IMicConsoleSession;
import cj.studio.gateway.mic.ISendResponse;
import cj.studio.gateway.mic.MicCommand;
import cj.studio.gateway.socket.Destination;
import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.cable.IGatewaySocketCable;
import cj.studio.gateway.socket.client.ClientGatewaySocket;
import cj.ultimate.util.StringUtil;

public class LsCommand extends MicCommand {

	@Override
	public String cmd() {
		return "ls";
	}

	@Override
	public String cmdDesc() {
		return "查看连接列表，用法：ls";
	}

	@Override
	public Options options() {
		Options options = new Options();
		Option f = new Option("f", "forward", false, "仅列出forward连结点");
		options.addOption(f);
		Option b = new Option("b", "backward", false, "仅列出backward连结点");
		options.addOption(b);
		Option s = new Option("s", "socket", false, "仅列出sockets");
		options.addOption(s);
		Option u = new Option("t", "tt", false, "开启即时监控");
		options.addOption(u);
		return options;
	}

	@Override
	public void doCommand(CommandLine line, String user, ISendResponse response,Frame frame, IMicConsoleSession session)
			throws CircuitException {
		IJunctionTable table = (IJunctionTable) session.provider().getService("$.junctions");
		IGatewaySocketContainer sockets = (IGatewaySocketContainer) session.provider().getService("$.container.socket");
		StringBuilder sb = new StringBuilder();
		sb.append("<ul>");
		if (line.hasOption("t")) {
			table.addForwardListener(new IJunctionListener() {
				@Override
				public void monitor(String action, Junction jun) {
					ForwardJunction fj = (ForwardJunction) jun;
					sb.append(action);
					switch (action) {
					case "A":
						printForwardJunction(sockets, fj, sb);
						break;
					case "R":
						printForwardJunction(sockets, fj, sb);
						break;
					}

				}
			});
			table.addBackwardListener(new IJunctionListener() {
				@Override
				public void monitor(String action, Junction jun) {
					BackwardJunction fj = (BackwardJunction) jun;
					sb.append(action);
					switch (action) {
					case "A":
						printBackwardJunction(sockets, fj, sb);
						break;
					case "R":
						printBackwardJunction(sockets, fj, sb);
						break;
					}

				}
			});
		}
		if (line.hasOption("f")) {
			Junction[] forwards = table.toSortedForwards();
			for (Junction jun : forwards) {
				if (jun == null)
					continue;
				ForwardJunction fj = (ForwardJunction) jun;
				printForwardJunction(sockets, fj, sb);
			}
			sb.append(String.format("<li>----------共%s个--------</li>", forwards.length));
			response.send(user, sb.toString());
			return;
		}
		if (line.hasOption("b")) {
			Junction[] backwards = table.toSortedBackwards();
			for (Junction jun : backwards) {
				if (jun == null)
					continue;
				BackwardJunction fj = (BackwardJunction) jun;
				printBackwardJunction(sockets, fj, sb);
			}
			sb.append(String.format("<li>----------共%s个--------</li>", backwards.length));
			response.send(user, sb.toString());
			return;
		}
		if (line.hasOption("s")) {
			IGatewaySocketContainer container = (IGatewaySocketContainer) session.provider().getService("$.container.socket");
			String[] names = container.enumSocketName();
			for (String name : names) {
				IGatewaySocket socket = container.find(name);
				printSocketInfo(socket, sb);
			}
			sb.append(String.format("<li>----------共%s个--------</li>", names.length));
			response.send(user, sb.toString());
			return;
		}
		Junction[] all = table.toSortedAll();
		for (Junction jun : all) {
			if (jun == null)
				continue;
			if (jun instanceof BackwardJunction) {
				BackwardJunction fj = (BackwardJunction) jun;
				printBackwardJunction(sockets, fj, sb);
			} else {
				ForwardJunction fj = (ForwardJunction) jun;
				printForwardJunction(sockets, fj, sb);
			}
		}
		sb.append("</ul>");
		response.send(user, sb.toString());
	}

	private void printSocketInfo(IGatewaySocket socket, StringBuilder sb) {
		String indent = "&nbsp;";
		sb.append(String.format("<li style='padding-left:5px;'>%s%s</li>", indent, socket.name()));
		sb.append(String.format("<li style='padding-left:5px;'>%s------------------------------------------------------------</li>", indent));
		sb.append(String.format("<li style='padding-left:40px;'>%s&nbsp;类型:%s</li>", indent, socket.getClass()));
		Destination dest = (Destination) socket.getService("$.destination");
		if (dest != null) {
			sb.append(String.format("<li style='padding-left:40px;'>%s&nbsp;目标属性:%s</li>", indent, dest.getProps()));
			sb.append(String.format("<li style='padding-left:40px;'>%s&nbsp;目标地址:%s</li>", indent, dest.getUris()));
		}
		if (socket instanceof ClientGatewaySocket) {
			ClientGatewaySocket cgs = (ClientGatewaySocket) socket;
			sb.append(String.format("<li style='padding-left:40px;'>%s&nbsp;线程池:%s</li>", indent, cgs.getService("$.socket.loopsize")));
			@SuppressWarnings("unchecked")
			List<IGatewaySocketCable> cables = (List<IGatewaySocketCable>) cgs.getService("$.cables");
			for (IGatewaySocketCable cable : cables) {
				sb.append(
						String.format("<li style='padding-left:40px;'>%s&nbsp;电缆:%s://%s:%s</li>", indent, cable.protocol(), cable.host(), cable.port()));
				sb.append(String.format("<li style='padding-left:60px;'>%s&nbsp;&nbsp;activedWireCount:%s</li>", indent,
						((IServiceProvider) cable).getService("$.wires.count")));
				sb.append(String.format("<li style='padding-left:60px;'>%s&nbsp;&nbsp;initialWireSize:%s</li>", indent, cable.initialWireSize()));
				sb.append(String.format("<li style='padding-left:60px;'>%s&nbsp;&nbsp;heartbeat:%s</li>", indent, cable.getHeartbeat()));
				sb.append(String.format("<li style='padding-left:60px;'>%s&nbsp;&nbsp;workThreadCount:%s</li>", indent, cable.workThreadCount()));
				if ("http".equals(cable.protocol()) || "https".equals(cable.protocol())) {
					sb.append(String.format("<li style='padding-left:60px;'>%s&nbsp;&nbsp;maxIdleConnections:%s</li>", indent,
							((IServiceProvider) cable).getService("$.prop.maxIdleConnections")));
					sb.append(String.format("<li style='padding-left:60px;'>%s&nbsp;&nbsp;keepAliveDuration:%s</li>", indent,
							((IServiceProvider) cable).getService("$.prop.keepAliveDuration")));
					sb.append(String.format("<li style='padding-left:60px;'>%s&nbsp;&nbsp;connectTimeout:%s</li>", indent,
							((IServiceProvider) cable).getService("$.prop.connectTimeout")));
					sb.append(String.format("<li style='padding-left:60px;'>%s&nbsp;&nbsp;readTimeout:%s</li>", indent,
							((IServiceProvider) cable).getService("$.prop.readTimeout")));
					sb.append(String.format("<li style='padding-left:60px;'>%s&nbsp;&nbsp;writeTimeout:%s</li>", indent,
							((IServiceProvider) cable).getService("$.prop.writeTimeout")));
					sb.append(String.format("<li style='padding-left:60px;'>%s&nbsp;&nbsp;followRedirects:%s</li>", indent,
							((IServiceProvider) cable).getService("$.prop.followRedirects")));
					sb.append(String.format("<li style='padding-left:60px;'>%s&nbsp;&nbsp;retryOnConnectionFailure:%s</li>", indent,
							((IServiceProvider) cable).getService("$.prop.retryOnConnectionFailure")));
				}
				if ("tcp".equals(cable.protocol()) || "udt".equals(cable.protocol())) {
					sb.append(String.format("<li style='padding-left:60px;'>%s&nbsp;&nbsp;acceptErrorPath:%s</li>", indent,
							((IServiceProvider) cable).getService("$.prop.acceptErrorPath")));
				}
			}
		}
	}

	private void printBackwardJunction(IGatewaySocketContainer sockets, BackwardJunction bj, StringBuilder sb) {
		String indent = "&nbsp;";
		sb.append(String.format("<li>%s管道：%s</li>", indent, bj.getName()));
		sb.append(String.format("<li style='padding-left:40px;'>%s&nbsp;网关中流向：backward %s->%s</li>", indent,
				bj.getFromWho(), bj.getToWho()));
		sb.append(String.format(
				"<li style='padding-left:40px;'>%s&nbsp;------------------------------------------------------------------------------------</li>",
				indent));
		sb.append(String.format("<li style='padding-left:80px;'>%s&nbsp;&nbsp;源点协议:%s</li>", indent,
				bj.getFromProtocol()));
		sb.append(
				String.format("<li style='padding-left:80px;'>%s&nbsp;&nbsp;目标协议:%s</li>", indent, bj.getToProtocol()));
		DateFormat format = new SimpleDateFormat("yy-MM-dd hh:mm:ss");
		sb.append(String.format("<li style='padding-left:80px;'>%s&nbsp;&nbsp;创建时间:%s</li>", indent,
				format.format(new Date(bj.getCreateTime()))));
		if (!StringUtil.isEmpty(bj.getLocalAddress())) {
			sb.append(String.format("<li style='padding-left:80px;'>%s&nbsp;&nbsp;本地地址:%s</li>", indent,
					bj.getLocalAddress()));
		}
		if (!StringUtil.isEmpty(bj.getRemoteAddress())) {
			sb.append(String.format("<li style='padding-left:80px;'>%s&nbsp;&nbsp;远程地址:%s</li>", indent,
					bj.getRemoteAddress()));
		}
		sb.append(String.format("<li style='padding-left:80px;'>%s&nbsp;&nbsp;目标类型:%s</li>", indent,
				bj.getToTargetClazz()));
		if (ClientGatewaySocket.class.isAssignableFrom(bj.getToTargetClazz())) {
			printTarget(sockets, bj.getToWho(), sb);
		}
	}

	private void printForwardJunction(IGatewaySocketContainer sockets, ForwardJunction fj, StringBuilder sb) {
		String indent = "&nbsp;";
		sb.append(String.format("<li>%s管道：%s</li>", indent, fj.getName()));
		sb.append(String.format("<li style='padding-left:40px;'>%s&nbsp;网关中流向：forward %s->%s</li>", indent,
				fj.getFromWho(), fj.getToWho()));
		sb.append(String.format(
				"<li style='padding-left:40px;'>%s&nbsp;------------------------------------------------------------------------------------</li>",
				indent));
		sb.append(String.format("<li style='padding-left:80px;'>%s&nbsp;&nbsp;源点协议:%s</li>", indent,
				fj.getFromProtocol()));
		sb.append(
				String.format("<li style='padding-left:80px;'>%s&nbsp;&nbsp;目标协议:%s</li>", indent, fj.getToProtocol()));
		DateFormat format = new SimpleDateFormat("yy-MM-dd hh:mm:ss");
		sb.append(String.format("<li style='padding-left:80px;'>%s&nbsp;&nbsp;创建时间:%s</li>", indent,
				format.format(new Date(fj.getCreateTime()))));
		sb.append(String.format("<li style='padding-left:80px;'>%s&nbsp;&nbsp;本地地址:%s</li>", indent,
				fj.getLocalAddress()));
		sb.append(String.format("<li style='padding-left:80px;'>%s&nbsp;&nbsp;远程地址:%s</li>", indent,
				fj.getRemoteAddress()));
		sb.append(String.format("<li style='padding-left:80px;'>%s&nbsp;&nbsp;目标类型:%s</li>", indent,
				fj.getToTargetClazz()));
		if (ClientGatewaySocket.class.isAssignableFrom(fj.getToTargetClazz())) {
			printTarget(sockets, fj.getToWho(), sb);
		}
	}

	private void printTarget(IGatewaySocketContainer sockets, String toWho, StringBuilder sb) {
		IGatewaySocket socket = sockets.find(toWho);
		if (socket == null) {
			return;
		}
		String indent = "&nbsp;";
		Destination dest = (Destination) socket.getService("$.destination");
		Map<String, String> props = dest.getProps();
		String[] keys = props.keySet().toArray(new String[0]);
		sb.append(String.format("<li style='padding-left:80px;'>%s&nbsp;&nbsp;&nbsp;属性:", indent));
		for (String key : keys) {
			if ("workThreadCount".equals(key)) {
				continue;
			}
			String v = props.get(key);
			sb.append(String.format("<li style='padding-left:120px;'>%s&nbsp;&nbsp;&nbsp;&nbsp;%s:%s</li>", indent, key,
					v));
		}
		int nThread = (int) socket.getService("$.socket.loopsize");
		sb.append(String.format("<li style='padding-left:120px;'>%s&nbsp;&nbsp;&nbsp;&nbsp;workThreadCount:%s</li>",
				indent, nThread));
		Object udtsize = socket.getService("$.socket.loopudtsize");
		if (udtsize != null) {
			int nThread_udt = (int) udtsize;
			sb.append(String.format(
					"<li style='padding-left:120px;'>%s&nbsp;&nbsp;&nbsp;&nbsp;workThreadCount_udt:%s</li>", indent,
					nThread_udt));
		}
		String[] uris = dest.getUris().toArray(new String[0]);
		sb.append(String.format("<li style='padding-left:80px;'>%s&nbsp;&nbsp;&nbsp;地址:", indent));
		for (String uri : uris) {
			sb.append(String.format("<li style='padding-left:120px;'>%s&nbsp;&nbsp;&nbsp;&nbsp;%s</li>", indent, uri));
		}
		@SuppressWarnings("unchecked")
		List<IGatewaySocketCable> cables = (List<IGatewaySocketCable>) socket.getService("$.cables");
		IGatewaySocketCable[] arr = cables.toArray(new IGatewaySocketCable[0]);
		sb.append(String.format("<li style='padding-left:80px;'>%s&nbsp;&nbsp;&nbsp;电缆:", indent));
		for (int i = 0; i < arr.length; i++) {
			sb.append(String.format(
					"<li style='padding-left:120px;'>%s&nbsp;&nbsp;&nbsp;&nbsp;%s -------------------------------------",
					indent, i));
			IGatewaySocketCable cable = arr[i];
			sb.append(String.format("<li style='padding-left:120px;'>%s&nbsp;&nbsp;&nbsp;&nbsp;uri=%s://%s:%s</li>",
					indent, cable.protocol(), cable.host(), cable.port()));
			sb.append(String.format("<li style='padding-left:120px;'>%s&nbsp;&nbsp;&nbsp;&nbsp;activedWires=%s</li>",
					indent, ((IServiceProvider) cable).getService("$.wires.count")));
			sb.append(String.format("<li style='padding-left:120px;'>%s&nbsp;&nbsp;&nbsp;&nbsp;heartbeat=%s</li>",
					indent, cable.getHeartbeat()));
			sb.append(String.format("<li style='padding-left:120px;'>%s&nbsp;&nbsp;&nbsp;&nbsp;initialWireSize=%s</li>",
					indent, cable.initialWireSize()));
			if ("http".equals(cable.protocol()) || "https".equals(cable.protocol())) {
				sb.append(String.format("<li style='padding-left:120px;'>%s&nbsp;&nbsp;&nbsp;&nbsp;maxIdleConnections:%s</li>",
						indent, ((IServiceProvider) cable).getService("$.prop.maxIdleConnections")));
				sb.append(String.format("<li style='padding-left:120px;'>%s&nbsp;&nbsp;&nbsp;&nbsp;keepAliveDuration:%s</li>",
						indent, ((IServiceProvider) cable).getService("$.prop.keepAliveDuration")));
				sb.append(String.format("<li style='padding-left:120px;'>%s&nbsp;&nbsp;&nbsp;&nbsp;connectTimeout:%s</li>", indent,
						((IServiceProvider) cable).getService("$.prop.connectTimeout")));
				sb.append(String.format("<li style='padding-left:120px;'>%s&nbsp;&nbsp;&nbsp;&nbsp;readTimeout:%s</li>", indent,
						((IServiceProvider) cable).getService("$.prop.readTimeout")));
				sb.append(String.format("<li style='padding-left:120px;'>%s&nbsp;&nbsp;&nbsp;&nbsp;writeTimeout:%s</li>", indent,
						((IServiceProvider) cable).getService("$.prop.writeTimeout")));
				sb.append(String.format("<li style='padding-left:120px;'>%s&nbsp;&nbsp;&nbsp;&nbsp;followRedirects:%s</li>", indent,
						((IServiceProvider) cable).getService("$.prop.followRedirects")));
				sb.append(String.format("<li style='padding-left:120px;'>%s&nbsp;&nbsp;&nbsp;&nbsp;retryOnConnectionFailure:%s</li>",
						indent, ((IServiceProvider) cable).getService("$.prop.retryOnConnectionFailure")));
			}
			if ("ws".equals(cable.protocol())) {
				sb.append(String.format("<li style='padding-left:120px;'>%s&nbsp;&nbsp;&nbsp;&nbsp;activedWires=%s</li>",
						indent, ((IServiceProvider) cable).getService("$.wspath")));
			}

		}
	}
}
