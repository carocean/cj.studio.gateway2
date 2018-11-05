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
import cj.studio.gateway.junction.ForwardJunction;
import cj.studio.gateway.junction.IJunctionListener;
import cj.studio.gateway.junction.Junction;
import cj.studio.gateway.tools.CmdLine;
import cj.studio.gateway.tools.Command;
import cj.studio.gateway.tools.Console;

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
					ForwardJunction fj=(ForwardJunction)jun;
					System.out.print(action);
					switch (action) {
					case "A":
						printForwardJunction(fj,indent);
						break;
					case "R":
						printForwardJunction(fj,indent);
						break;
					}

				}
			});
		}
		String[] forwardNames=table.enumForwardName();
		for (String name:forwardNames) {
			ForwardJunction fj = (ForwardJunction) table.findInForwards(name);
			if(fj==null)continue;
			System.out.println(String.format("%s管道：%s", indent, fj.getName()));
			printForwardJunction(fj, indent);
			System.out.println();
		}
	}

	private void printForwardJunction(ForwardJunction fj, String indent) {
		System.out.println(String.format("%s\t网关中流向：forward %s->%s", indent,fj.getNetName(),fj.getDestName()));
		System.out.println(String.format("%s\t--------------------", indent));
		System.out.println(String.format("%s\t\t网络协议:%s", indent,fj.getProtocol()));
		DateFormat format = new SimpleDateFormat("yy-MM-dd hh:mm:ss");
		System.out.println(String.format("%s\t\t创建时间:%s", indent, format.format(new Date(fj.getCreateTime()))));
		System.out.println(String.format("%s\t\t本地地址:%s", indent, fj.getLocalAddress()));
		System.out.println(String.format("%s\t\t远程地址:%s", indent, fj.getRemoteAddress()));
		System.out.println(String.format("%s\t\t目标类型:%s", indent, fj.getTargetClazz()));
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
		// Option name = new Option("v", "virdomain",false, "仅列出虚域的配置信息");
		// options.addOption(name);
		Option u = new Option("t", "tt", false, "开启即时监控");
		options.addOption(u);
		// Option p = new Option("p", "password",true, "密码");
		// options.addOption(p);
		return options;
	}
}
