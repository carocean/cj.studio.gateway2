package cj.studio.gateway.mic;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.net.CircuitException;
import cj.ultimate.IDisposable;

class MicConsoleSession implements IMicConsoleSession, IDisposable {

	IServiceProvider parent;
	Map<String, IMicConsole> currents;
	Stack<IMicConsole> history;

	public MicConsoleSession(IServiceProvider parent) {
		this.parent = parent;
		currents = new HashMap<>();
		history = new Stack<>();
	}

	@Override
	public IMicConsole current(String user) throws CircuitException {
		return currents.get(user);
	}

	@Override
	public void cd(String user, IMicConsole console) throws CircuitException {
		cdImpl(user,console);
		ISendResponse response = (ISendResponse) parent.getService("$.mic.response");
		response.onCDConsole(user, console.name());
	}
	private void cdImpl(String user, IMicConsole console) throws CircuitException{
		if (!console.isInited(user)) {
			console.init(user, parent);
		}
		IMicConsole current = currents.get(user);
		if (current != null) {
			history.push(current);
		}
		currents.put(user, console);
	}
	@Override
	public final void bye(String user) throws CircuitException {
		if (history.isEmpty())
			return;
		try {
			IMicConsole console = history.pop();
			cdImpl(user,console);
			ISendResponse response = (ISendResponse) parent.getService("$.mic.response");
			response.onByeConsole(user, console.name());
		} catch (CircuitException e) {
			ISendResponse response = (ISendResponse) parent.getService("$.mic.response");
			response.send(user, e.getMessage());
			e.printStackTrace();
		}

	}

	@Override
	public IServiceProvider provider() {
		return parent;
	}

	@Override
	public void dispose() {
		this.currents.clear();
		this.parent = null;
	}
}