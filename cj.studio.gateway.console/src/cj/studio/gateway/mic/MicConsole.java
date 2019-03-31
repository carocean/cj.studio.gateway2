package cj.studio.gateway.mic;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.net.CircuitException;

public abstract class MicConsole implements IMicConsole {
	private Map<String, MicCommand> commands;
	private ISendResponse response;
	private String user;
	private boolean isInited;
	String name;
	public MicConsole(String name) {
		commands = new HashMap<>();
		this.name=name;
	}
	@Override
	public String name() {
		return name;
	}
	protected final String user() {
		return user;
	}

	protected final ISendResponse response() {
		return response;
	}
	@Override
	public final boolean isInited(String user) {
		return isInited;
	}
	@Override
	public final void printMan() throws CircuitException {
		StringWriter out = new StringWriter();
		PrintWriter pw = new PrintWriter(out);
		pw.append(String.format("<div><span>%s</span></div>",manHeader()));
		pw.append("<br>");
		pw.append("<ul>");
		for (String cmdName : commands.keySet()) {
			MicCommand cmd = commands.get(cmdName);
			IHelpFormatter formatter = new MicHelpFormatter();
			if (cmd.options() != null) {
				formatter.printHelp(pw, cmd);
			}
			pw.append("<li>------------------------------</li>");
		}
		pw.append("</ul>");
		pw.flush();
		String usage = out.getBuffer().toString();
		response.send(user, usage);
		pw.close();
	}

	protected abstract String manHeader();

	@Override
	public final void init(String user, IServiceProvider parent) throws CircuitException {
		response = (ISendResponse) parent.getService("$.mic.response");
		this.user = user;
		List<MicCommand> cmds=new ArrayList<>();
		register(cmds);
		for(MicCommand cmd:cmds) {
			if(this.commands.containsKey(cmd.cmd())) {
				String error=String.format("已存在命令：%s，类型：%s", cmd.cmd(),cmd);
				response.send(user, error);
				throw new CircuitException("503", error);
			}
			this.commands.put(cmd.cmd(), cmd);
		}
		isInited=true;
	}

	protected abstract void register(List<MicCommand> registry);

	@Override
	public final MicCommand get(String cmdName) {
		return commands.get(cmdName);
	}

}
