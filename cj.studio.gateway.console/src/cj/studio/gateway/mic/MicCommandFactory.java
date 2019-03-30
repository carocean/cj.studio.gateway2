package cj.studio.gateway.mic;

import java.util.HashMap;
import java.util.Map;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IInputChannel;
import cj.studio.ecm.net.IOutputChannel;
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.ecm.net.io.MemoryInputChannel;
import cj.studio.ecm.net.io.MemoryOutputChannel;
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
	public void exeCommand(String cmdline,String user) throws CircuitException {
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
			sendResponse(user,"不认识的命令：" + cmdName);
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

	private void sendResponse(String user,String response) throws CircuitException {
		MicRegistry registry=(MicRegistry)parent.getService("$.registry");
		IInputChannel in = new MemoryInputChannel();
		Frame f = new Frame(in, "register /mic/response.service mic/1.0");
		f.parameter("cjtoken",registry.getMic().getCjtoken());
		f.parameter("user",user);
		f.content().accept(new MemoryContentReciever());
		in.begin(f);
		byte[] b=response.getBytes();
		in.done(b, 0, b.length);
		
		IOutputChannel output=new MemoryOutputChannel();
		Circuit c=new Circuit(output, "mic/1.0 200 OK");
		input.headFlow(f, c);
	}

}
