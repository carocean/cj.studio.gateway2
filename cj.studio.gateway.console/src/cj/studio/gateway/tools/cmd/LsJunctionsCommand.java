package cj.studio.gateway.tools.cmd;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceInvertInjection;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.gateway.IGateway;
import cj.studio.gateway.IJunctionTable;
import cj.studio.gateway.junction.BackwardJunction;
import cj.studio.gateway.junction.ForwardJunction;
import cj.studio.gateway.junction.IJunctionListener;
import cj.studio.gateway.junction.Junction;
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
						printForwardJunction(fj, indent);
						break;
					case "R":
						printForwardJunction(fj, indent);
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
						printBackwardJunction(fj, indent);
						break;
					case "R":
						printBackwardJunction(fj, indent);
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
				printForwardJunction(fj, indent);
				System.out.println();
			}
			return;
		}
		if (line.hasOption("b")) {
			Junction[] backwards = table.toSortedBackwards();
			for (Junction jun : backwards) {
				if (jun == null)
					continue;
				BackwardJunction fj = (BackwardJunction) jun;
				printBackwardJunction(fj, indent);
				System.out.println();
			}
			return;
		}
		Junction[] all = table.toSortedAll();
		for (Junction jun : all) {
			if (jun == null)
				continue;
			if (jun instanceof BackwardJunction) {
				BackwardJunction fj = (BackwardJunction) jun;
				printBackwardJunction(fj, indent);
			} else {
				ForwardJunction fj = (ForwardJunction) jun;
				printForwardJunction(fj, indent);
			}
			System.out.println();
		}
	}

	private void printBackwardJunction(BackwardJunction bj, String indent) {
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
	}

	private void printForwardJunction(ForwardJunction fj, String indent) {
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
		Option u = new Option("t", "tt", false, "开启即时监控");
		options.addOption(u);
		// Option p = new Option("p", "password",true, "密码");
		// options.addOption(p);
		return options;
	}
}
