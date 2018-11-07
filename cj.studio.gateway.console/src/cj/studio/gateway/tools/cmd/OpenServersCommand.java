package cj.studio.gateway.tools.cmd;

import java.io.IOException;

import org.apache.commons.cli.Options;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceInvertInjection;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.gateway.IGateway;
import cj.studio.gateway.tools.CmdLine;
import cj.studio.gateway.tools.Command;
import cj.studio.gateway.tools.Console;

@CjService(name = "openServersCommand")
public class OpenServersCommand extends Command {

	@CjServiceInvertInjection
	@CjServiceRef(refByName = "routerConsole")
	Console routerConsole;
	@CjServiceRef(refByName = "serversConsole")
	Console console;
	public void doCommand(CmdLine cl) throws IOException {
		IGateway gateway = (IGateway) cl.prop("gateway");
		String indent = cl.propString("indent");
		console.monitor(gateway, indent);
	}

	@Override
	public String cmdDesc() {
		return "进入服务器配置窗口";
	}

	@Override
	public String cmd() {
		return "sc";
	}

	@Override
	public Options options() {
		Options options = new Options();
		// Option name = new Option("n", "name",true, "网盘名");
		// options.addOption(name);
		// Option u = new Option("u", "user",true, "用户名");
		// options.addOption(u);
		// Option p = new Option("p", "password",true, "密码");
		// options.addOption(p);
		return options;
	}
}
