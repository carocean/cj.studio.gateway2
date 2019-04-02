package cj.studio.gateway.mic;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.ParseException;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IInputChannel;
import cj.studio.ecm.net.IOutputChannel;
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.ecm.net.io.MemoryInputChannel;
import cj.studio.ecm.net.io.MemoryOutputChannel;
import cj.studio.gateway.mic.cmd.MainConsole;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import cj.ultimate.util.StringUtil;

class MicCommandFactory implements IMicCommandFactory, IServiceProvider {
	IServiceProvider parent;

	ISendResponse response;
	IMicConsoleSession session;

	public MicCommandFactory(IServiceProvider parent) {
		this.parent = parent;
		response = new SendResponse(parent);
		session = new MicConsoleSession(this);
	}

	@Override
	public Object getService(String serviceId) {
		if ("$.mic.response".equals(serviceId)) {
			return response;
		}
		return parent.getService(serviceId);
	}

	@Override
	public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {
		return parent.getServices(serviceClazz);
	}

	@Override
	public void exeCommand(String cmdline, String user,Frame frame) throws CircuitException {
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
		IMicConsole console = session.current(user);
		if (console == null) {
			console=new MainConsole();
			session.cd(user, console);
		}
		if ("man".equals(cmdName)) {
			console.printMan();
			return;
		}
		if("prefix".equals(cmdName)) {
			response.send(user, String.format("$prefix{%s}",console.name()));
			return;
		}
		String args[] = argline.split(" ");
		MicCommand cmd = console.get(cmdName);
		if(cmd==null) {
			String err="命令不存在："+cmdName;
			response.send(user, err);
			throw new CircuitException("503", err);
		}
		GnuParser parser = new GnuParser();
		try {
			CommandLine the = parser.parse(cmd.options(), args);
			cmd.doCommand(the, user, response,frame, session);
		} catch (ParseException e) {
			response.send(user, e.getMessage());
			throw new CircuitException("503", e);
		}

	}

	class SendResponse implements ISendResponse {
		IServiceProvider parent;
		IInputPipeline input;

		public SendResponse(IServiceProvider parent) {
			this.parent = parent;
			input = (IInputPipeline) parent.getService("$.sender.input");
		}

		@Override
		public void send(String user, String response) throws CircuitException {
			MicRegistry registry = (MicRegistry) parent.getService("$.registry");
			IInputChannel in = new MemoryInputChannel();
			Frame f = new Frame(in, "register /mic/response.service mic/1.0");
			f.parameter("cjtoken", registry.getMic().getCjtoken());
			f.parameter("user", user);
			f.content().accept(new MemoryContentReciever());
			in.begin(f);
			byte[] b = response.getBytes();
			in.done(b, 0, b.length);

			IOutputChannel output = new MemoryOutputChannel();
			Circuit c = new Circuit(output, "mic/1.0 200 OK");

			input.headFlow(f, c);
		}
		@Override
		public void onCDConsole(String user, String consoleName) throws CircuitException {
			MicRegistry registry = (MicRegistry) parent.getService("$.registry");
			IInputChannel in = new MemoryInputChannel();
			Frame f = new Frame(in, "register /mic/onCDConsole.service mic/1.0");
			f.parameter("cjtoken", registry.getMic().getCjtoken());
			f.parameter("user", user);
			f.parameter("consoleName", consoleName);
			f.content().accept(new MemoryContentReciever());
			in.begin(f);
			byte[] b = new byte[0];
			in.done(b, 0, b.length);

			IOutputChannel output = new MemoryOutputChannel();
			Circuit c = new Circuit(output, "mic/1.0 200 OK");

			input.headFlow(f, c);
		}
		@Override
		public void onByeConsole(String user, String consoleName) throws CircuitException{
			MicRegistry registry = (MicRegistry) parent.getService("$.registry");
			IInputChannel in = new MemoryInputChannel();
			Frame f = new Frame(in, "register /mic/onByeConsole.service mic/1.0");
			f.parameter("cjtoken", registry.getMic().getCjtoken());
			f.parameter("user", user);
			f.parameter("consoleName", consoleName);
			f.content().accept(new MemoryContentReciever());
			in.begin(f);
			byte[] b = new byte[0];
			in.done(b, 0, b.length);

			IOutputChannel output = new MemoryOutputChannel();
			Circuit c = new Circuit(output, "mic/1.0 200 OK");

			input.headFlow(f, c);
		}
	}
}
