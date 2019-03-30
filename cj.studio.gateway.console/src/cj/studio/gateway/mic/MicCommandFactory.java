package cj.studio.gateway.mic;

import java.util.HashMap;
import java.util.Map;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import cj.studio.gateway.tools.Command;
import cj.ultimate.util.StringUtil;

class MicCommandFactory implements IMicCommandFactory {
	IServiceProvider parent;
	protected Map<String, Command> commands;
	IInputPipeline input;
	public MicCommandFactory(IServiceProvider parent) {
		this.parent = parent;
		commands = new HashMap<>();
		input=(IInputPipeline)parent.getService("$.sender.input");
	}

	@Override
	public void exeCommand(String cmdline,String channel) throws CircuitException {
		if (StringUtil.isEmpty(cmdline)) {
			return;
		}
		while (cmdline.startsWith(" ")) {
			cmdline = cmdline.substring(1, cmdline.length());
		}
		int pos = cmdline.indexOf(" ");
		String cmdName = "";
		String argline = "";
		if (pos > -1) {
			cmdName = cmdline.substring(0, pos);
			argline = cmdline.substring(pos + 1, cmdline.length());
		} else {
			cmdName = cmdline;
		}
//			if ("man".equals(cmdName)) {
//				printMan(commands);
//				continue;
//			}
		if (!commands.containsKey(cmdName)) {
			throw new EcmException("不认识的命令：" + cmdName);
		}
		String args[] = argline.split(" ");
//			Command cmd = commands.get(cmdName);
//			GnuParser parser = new GnuParser();
//			try {
//				CommandLine the = parser.parse(cmd.options(), args);
//			} catch (ParseException e) {
//				e.printStackTrace();
//			}

	}

}
