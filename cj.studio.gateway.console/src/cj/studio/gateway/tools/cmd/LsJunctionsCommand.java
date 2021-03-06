package cj.studio.gateway.tools.cmd;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceInvertInjection;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.gateway.IGateway;
import cj.studio.gateway.IGatewaySocketContainer;
import cj.studio.gateway.IJunctionTable;
import cj.studio.gateway.junction.BackwardJunction;
import cj.studio.gateway.junction.ForwardJunction;
import cj.studio.gateway.junction.IJunctionListener;
import cj.studio.gateway.junction.Junction;
import cj.studio.gateway.socket.Destination;
import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.cable.IGatewaySocketCable;
import cj.studio.gateway.socket.client.ClientGatewaySocket;
import cj.studio.gateway.tools.CmdLine;
import cj.studio.gateway.tools.Command;
import cj.studio.gateway.tools.Console;
import cj.ultimate.util.StringUtil;

@CjService(name = "LsJunctionsCommand")
public class LsJunctionsCommand extends Command {

	@CjServiceInvertInjection
	@CjServiceRef(refByName = "routerConsole")
	Console routerConsole;

	public void doCommand(CmdLine cl) throws IOException {
		IGateway gateway = (IGateway) cl.prop("gateway");
		IJunctionTable table = (IJunctionTable) gateway.getService("$.junctions");
		final IGatewaySocketContainer sockets = (IGatewaySocketContainer) gateway.getService("$.container.socket");
		String indent = cl.propString("indent");
		CommandLine line = cl.line();

		System.out.println("------------打印连结列表－－－－－－－－－");
		System.out.println();
		if (line.hasOption("t")) {
			table.addForwardListener(new IJunctionListener() {
				@Override
				public void monitor(String action, Junction jun) {
					ForwardJunction fj = (ForwardJunction) jun;
					System.out.print(action);
					switch (action) {
					case "A":
						printForwardJunction(sockets, fj, indent);
						break;
					case "R":
						printForwardJunction(sockets, fj, indent);
						break;
					}

				}
			});
			table.addBackwardListener(new IJunctionListener() {
				@Override
				public void monitor(String action, Junction jun) {
					BackwardJunction fj = (BackwardJunction) jun;
					System.out.print(action);
					switch (action) {
					case "A":
						printBackwardJunction(sockets, fj, indent);
						break;
					case "R":
						printBackwardJunction(sockets, fj, indent);
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
				printForwardJunction(sockets, fj, indent);
			}
			System.out.println(String.format("----------共%s个--------", forwards.length));
			return;
		}
		if (line.hasOption("b")) {
			Junction[] backwards = table.toSortedBackwards();
			for (Junction jun : backwards) {
				if (jun == null)
					continue;
				BackwardJunction fj = (BackwardJunction) jun;
				printBackwardJunction(sockets, fj, indent);
			}
			System.out.println(String.format("----------共%s个--------", backwards.length));
			return;
		}
		if (line.hasOption("s")) {
			IGatewaySocketContainer container = (IGatewaySocketContainer) gateway.getService("$.container.socket");
			String[] names = container.enumSocketName();
			for (String name : names) {
				IGatewaySocket socket = container.find(name);
				printSocketInfo(socket, indent);
			}
			System.out.println(String.format("----------共%s个--------", names.length));
			return;
		}
		Junction[] all = table.toSortedAll();
		for (Junction jun : all) {
			if (jun == null)
				continue;
			if (jun instanceof BackwardJunction) {
				BackwardJunction fj = (BackwardJunction) jun;
				printBackwardJunction(sockets, fj, indent);
			} else {
				ForwardJunction fj = (ForwardJunction) jun;
				printForwardJunction(sockets, fj, indent);
			}
		}
		System.out.println(String.format("----------共%s个--------", all.length));
	}

	private void printSocketInfo(IGatewaySocket socket, String indent) {
		System.out.println(String.format("%s%s", indent, socket.name()));
		System.out.println(String.format("%s--------------------", indent));
		System.out.println(String.format("%s\t类型:%s", indent, socket.getClass()));
		Destination dest = (Destination) socket.getService("$.destination");
		if (dest != null) {
			System.out.println(String.format("%s\t目标属性:%s", indent, dest.getProps()));
			System.out.println(String.format("%s\t目标地址:%s", indent, dest.getUris()));
		}
		if (socket instanceof ClientGatewaySocket) {
			ClientGatewaySocket cgs = (ClientGatewaySocket) socket;
			System.out.println(String.format("%s\t线程池:%s", indent, cgs.getService("$.socket.loopsize")));
			@SuppressWarnings("unchecked")
			List<IGatewaySocketCable> cables = (List<IGatewaySocketCable>) cgs.getService("$.cables");
			for (IGatewaySocketCable cable : cables) {
				System.out.println(
						String.format("%s\t电缆:%s://%s:%s", indent, cable.protocol(), cable.host(), cable.port()));
				System.out.println(String.format("%s\t\tactivedWireCount:%s", indent,
						((IServiceProvider) cable).getService("$.wires.count")));
				System.out.println(String.format("%s\t\tinitialWireSize:%s", indent, cable.initialWireSize()));
				System.out.println(String.format("%s\t\theartbeat:%s", indent, cable.getHeartbeat()));
				System.out.println(String.format("%s\t\tworkThreadCount:%s", indent, cable.workThreadCount()));
				if ("http".equals(cable.protocol()) || "https".equals(cable.protocol())) {
					System.out.println(String.format("%s\t\tmaxIdleConnections:%s", indent,
							((IServiceProvider) cable).getService("$.prop.maxIdleConnections")));
					System.out.println(String.format("%s\t\tkeepAliveDuration:%s", indent,
							((IServiceProvider) cable).getService("$.prop.keepAliveDuration")));
					System.out.println(String.format("%s\t\tconnectTimeout:%s", indent,
							((IServiceProvider) cable).getService("$.prop.connectTimeout")));
					System.out.println(String.format("%s\t\treadTimeout:%s", indent,
							((IServiceProvider) cable).getService("$.prop.readTimeout")));
					System.out.println(String.format("%s\t\twriteTimeout:%s", indent,
							((IServiceProvider) cable).getService("$.prop.writeTimeout")));
					System.out.println(String.format("%s\t\tfollowRedirects:%s", indent,
							((IServiceProvider) cable).getService("$.prop.followRedirects")));
					System.out.println(String.format("%s\t\tretryOnConnectionFailure:%s", indent,
							((IServiceProvider) cable).getService("$.prop.retryOnConnectionFailure")));
				}
				if("tcp".equals(cable.protocol())||"udt".equals(cable.protocol())) {
					System.out.println(String.format("%s\t\tacceptErrorPath:%s", indent,
							((IServiceProvider) cable).getService("$.prop.acceptErrorPath")));
				}
			}
		}
	}

	private void printBackwardJunction(IGatewaySocketContainer sockets, BackwardJunction bj, String indent) {
		System.out.println(String.format("%s管道：%s", indent, bj.getName()));
		System.out.println(String.format("%s\t网关中流向：backward %s->%s", indent, bj.getFromWho(), bj.getToWho()));
		System.out.println(String.format("%s\t--------------------", indent));
		System.out.println(String.format("%s\t\t源点协议:%s", indent, bj.getFromProtocol()));
		System.out.println(String.format("%s\t\t目标协议:%s", indent, bj.getToProtocol()));
		DateFormat format = new SimpleDateFormat("yy-MM-dd hh:mm:ss");
		System.out.println(String.format("%s\t\t创建时间:%s", indent, format.format(new Date(bj.getCreateTime()))));
		if (!StringUtil.isEmpty(bj.getLocalAddress())) {
			System.out.println(String.format("%s\t\t本地地址:%s", indent, bj.getLocalAddress()));
		}
		if (!StringUtil.isEmpty(bj.getRemoteAddress())) {
			System.out.println(String.format("%s\t\t远程地址:%s", indent, bj.getRemoteAddress()));
		}
		System.out.println(String.format("%s\t\t目标类型:%s", indent, bj.getToTargetClazz()));
		if (ClientGatewaySocket.class.isAssignableFrom(bj.getToTargetClazz())) {
			printTarget(sockets, bj.getToWho(), indent);
		}
	}

	private void printForwardJunction(IGatewaySocketContainer sockets, ForwardJunction fj, String indent) {
		System.out.println(String.format("%s管道：%s", indent, fj.getName()));
		System.out.println(String.format("%s\t网关中流向：forward %s->%s", indent, fj.getFromWho(), fj.getToWho()));
		System.out.println(String.format("%s\t--------------------", indent));
		System.out.println(String.format("%s\t\t源点协议:%s", indent, fj.getFromProtocol()));
		System.out.println(String.format("%s\t\t目标协议:%s", indent, fj.getToProtocol()));
		DateFormat format = new SimpleDateFormat("yy-MM-dd hh:mm:ss");
		System.out.println(String.format("%s\t\t创建时间:%s", indent, format.format(new Date(fj.getCreateTime()))));
		System.out.println(String.format("%s\t\t本地地址:%s", indent, fj.getLocalAddress()));
		System.out.println(String.format("%s\t\t远程地址:%s", indent, fj.getRemoteAddress()));
		System.out.println(String.format("%s\t\t目标类型:%s", indent, fj.getToTargetClazz()));
		if (ClientGatewaySocket.class.isAssignableFrom(fj.getToTargetClazz())) {
			printTarget(sockets, fj.getToWho(), indent);
		}
	}

	private void printTarget(IGatewaySocketContainer sockets, String toWho, String indent) {
		IGatewaySocket socket = sockets.find(toWho);
		if (socket == null) {
			return;
		}
		Destination dest = (Destination) socket.getService("$.destination");
		Map<String, String> props = dest.getProps();
		String[] keys = props.keySet().toArray(new String[0]);
		System.out.println(String.format("%s\t\t\t属性:", indent));
		for (String key : keys) {
			if ("workThreadCount".equals(key)) {
				continue;
			}
			String v = props.get(key);
			System.out.println(String.format("%s\t\t\t\t%s:%s", indent, key, v));
		}
		int nThread = (int) socket.getService("$.socket.loopsize");
		System.out.println(String.format("%s\t\t\t\tworkThreadCount:%s", indent, nThread));
		Object udtsize = socket.getService("$.socket.loopudtsize");
		if (udtsize != null) {
			int nThread_udt = (int) udtsize;
			System.out.println(String.format("%s\t\t\t\tworkThreadCount_udt:%s", indent, nThread_udt));
		}
		String[] uris = dest.getUris().toArray(new String[0]);
		System.out.println(String.format("%s\t\t\t地址:", indent));
		for (String uri : uris) {
			System.out.println(String.format("%s\t\t\t\t%s", indent, uri));
		}
		@SuppressWarnings("unchecked")
		List<IGatewaySocketCable> cables = (List<IGatewaySocketCable>) socket.getService("$.cables");
		IGatewaySocketCable[] arr = cables.toArray(new IGatewaySocketCable[0]);
		System.out.println(String.format("%s\t\t\t电缆:", indent));
		for (int i = 0; i < arr.length; i++) {
			System.out.println(String.format("%s\t\t\t\t%s -------------------------------------", indent, i));
			IGatewaySocketCable cable = arr[i];
			System.out.println(
					String.format("%s\t\t\t\turi=%s://%s:%s", indent, cable.protocol(), cable.host(), cable.port()));
			System.out.println(String.format("%s\t\t\t\tactivedWires=%s", indent,
					((IServiceProvider) cable).getService("$.wires.count")));
			System.out.println(String.format("%s\t\t\t\theartbeat=%s", indent, cable.getHeartbeat()));
			System.out.println(String.format("%s\t\t\t\tinitialWireSize=%s", indent, cable.initialWireSize()));
			if ("http".equals(cable.protocol()) || "https".equals(cable.protocol())) {
				System.out.println(String.format("%s\t\tmaxIdleConnections:%s", indent,
						((IServiceProvider) cable).getService("$.prop.maxIdleConnections")));
				System.out.println(String.format("%s\t\tkeepAliveDuration:%s", indent,
						((IServiceProvider) cable).getService("$.prop.keepAliveDuration")));
				System.out.println(String.format("%s\t\tconnectTimeout:%s", indent,
						((IServiceProvider) cable).getService("$.prop.connectTimeout")));
				System.out.println(String.format("%s\t\treadTimeout:%s", indent,
						((IServiceProvider) cable).getService("$.prop.readTimeout")));
				System.out.println(String.format("%s\t\twriteTimeout:%s", indent,
						((IServiceProvider) cable).getService("$.prop.writeTimeout")));
				System.out.println(String.format("%s\t\tfollowRedirects:%s", indent,
						((IServiceProvider) cable).getService("$.prop.followRedirects")));
				System.out.println(String.format("%s\t\tretryOnConnectionFailure:%s", indent,
						((IServiceProvider) cable).getService("$.prop.retryOnConnectionFailure")));
			}
			if ("ws".equals(cable.protocol())) {
				System.out.println(String.format("%s\t\t\t\tactivedWires=%s", indent,
						((IServiceProvider) cable).getService("$.wspath")));
			}

		}
	}

	@Override
	public String cmdDesc() {
		return "列出连结表";
	}

	@Override
	public String cmd() {
		return "ls";
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
		// Option p = new Option("p", "password",true, "密码");
		// options.addOption(p);
		return options;
	}
}
