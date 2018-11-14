package cj.studio.gateway.socket.pipeline;

import java.util.HashMap;
import java.util.Map;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.graph.CircuitException;

public class InputPipeline implements IInputPipeline {
	LinkEntry head;
	LinkEntry last;
	IIPipeline adapter;
	Map<String, String> props;
	private boolean disposed;

	public InputPipeline(IInputValve first, IInputValve last) {
		head = new LinkEntry(first);
		head.next = new LinkEntry(last);
		this.last = head.next;
		this.adapter = new IPipeline(this);
	}

	@Override
	public String prop(String name) {
		if (props == null)
			return null;
		return props.get(name);
	}

	@Override
	public void prop(String name, String value) {
		if (props == null)
			props = new HashMap<>();
		props.put(name, value);
	}

	@Override
	public void add(IInputValve valve) {
		LinkEntry entry = getEndConstomerEntry();
		if (entry == null) {
			return;
		}
		LinkEntry lastEntry = entry.next;
		entry.next = new LinkEntry(valve);
		entry.next.next = lastEntry;
	}

	private LinkEntry getEndConstomerEntry() {
		if (head == null)
			return null;
		LinkEntry tmp = head;
		do {
			if (last.equals(tmp.next)) {
				return tmp;
			}
			tmp = tmp.next;
		} while (tmp.next != null);
		return null;
	}

	@Override
	public void remove(IInputValve valve) {
		LinkEntry tmp = head;
		do {
			if (valve.equals(tmp.next.entry)) {
				break;
			}
			tmp = tmp.next;
		} while (tmp.next != null);
		tmp.next = tmp.next.next;
	}

	@Override
	public void headFlow(Object request, Object response) throws CircuitException {
		try {
			nextFlow(request, response, null);
		} catch (Throwable e) {
			CircuitException ce = CircuitException.search(e);
			if (ce != null) {
				if (response instanceof Circuit) {
					Circuit c = (Circuit) response;
					c.status(ce.getStatus());
					c.message(ce.messageCause());
				}
				throw ce;
			}
			if (response instanceof Circuit) {
				Circuit c = (Circuit) response;
				c.status("503");
				c.message(e.getMessage());
			}
			throw e;
		}
	}

	@Override
	public void nextFlow(Object request, Object response, IInputValve formthis) throws CircuitException {
		if (formthis == null) {
			head.entry.flow(request, response, this);
			return;
		}
		LinkEntry linkEntry = lookforHead(formthis);
		if (linkEntry == null || linkEntry.next == null)
			return;
		linkEntry.next.entry.flow(request, response, this);
	}

	private LinkEntry lookforHead(IInputValve formthis) {
		if (head == null)
			return null;
		LinkEntry tmp = head;
		do {
			if (formthis.equals(tmp.entry)) {
				break;
			}
			tmp = tmp.next;
		} while (tmp.next != null);
		return tmp;
	}

	@Override
	public void dispose() {
		LinkEntry next = head;
		LinkEntry prev = null;
		while (next.next != null) {
			prev = next;
			next = next.next;
			prev.next = null;
			prev.entry = null;
		}
		this.head = null;
		this.last = null;
		disposed = true;
	}

	@Override
	public boolean isDisposed() {
		return disposed;
	}

	class LinkEntry {
		LinkEntry next;
		IInputValve entry;

		public LinkEntry(IInputValve entry) {
			this.entry = entry;
		}

	}

	@Override
	public void headOnActive(String inputName) throws CircuitException {
		nextOnActive(inputName, null);
	}

	@Override
	public void headOnInactive(String inputName) throws CircuitException {
		nextOnInactive(inputName, null);

	}

	@Override
	public void nextOnActive(String inputName, IInputValve formthis) throws CircuitException {
		if (formthis == null) {
			head.entry.onActive(inputName, this);
			return;
		}
		LinkEntry linkEntry = lookforHead(formthis);
		if (linkEntry == null || linkEntry.next == null)
			return;
		linkEntry.next.entry.onActive(inputName, this);

	}

	@Override
	public void nextOnInactive(String inputName, IInputValve formthis) throws CircuitException {
		if (formthis == null) {
			head.entry.onInactive(inputName, this);
			return;
		}
		LinkEntry linkEntry = lookforHead(formthis);
		if (linkEntry == null || linkEntry.next == null)
			return;
		linkEntry.next.entry.onInactive(inputName, this);
	}

	class IPipeline implements IIPipeline {
		IInputPipeline target;

		public IPipeline(IInputPipeline target) {
			this.target = target;
		}

		@Override
		public void nextFlow(Object request, Object response, IInputValve formthis) throws CircuitException {
			target.nextFlow(request, response, formthis);
		}

		@Override
		public void nextOnActive(String inputName, IInputValve formthis) throws CircuitException {
			target.nextOnActive(inputName, formthis);

		}

		@Override
		public void nextOnInactive(String inputName, IInputValve formthis) throws CircuitException {
			target.nextOnInactive(inputName, formthis);

		}

		@Override
		public String prop(String name) {
			return target.prop(name);
		}
	}

}