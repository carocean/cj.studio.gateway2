package cj.studio.gateway.socket.pipeline;

import java.util.HashMap;
import java.util.Map;

import cj.studio.ecm.graph.CircuitException;
import cj.ultimate.IClosable;

public class OutputPipeline implements IOutputPipeline {
	LinkEntry head;
	LinkEntry last;
	IOPipeline adapter;
	Map<String, String> props;
	Outputer handler;
	private boolean disposed;

	public OutputPipeline(IOutputValve first, IOutputValve last) {
		head = new LinkEntry(first);
		head.next = new LinkEntry(last);
		this.last = head.next;
		this.adapter = new OPipeline(this);
		this.handler = new Outputer(this);
	}

	@Override
	public void add(IOutputValve valve) {
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
	public void remove(IOutputValve valve) {
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
		nextFlow(request, response, null);
	}

	@Override
	public void headOnActive() throws CircuitException {
		nextOnActive(null);

	}

	@Override
	public void headOnInactive() throws CircuitException {
		nextOnInactive(null);
	}

	@Override
	public void nextOnActive(IOutputValve formthis) throws CircuitException {
		if (formthis == null) {
			head.entry.onActive(this);
			return;
		}
		LinkEntry linkEntry = lookforHead(formthis);
		if (linkEntry == null || linkEntry.next == null)
			return;
		linkEntry.next.entry.onActive(this);

	}

	@Override
	public void nextOnInactive(IOutputValve formthis) throws CircuitException {
		if (formthis == null) {
			head.entry.onInactive(this);
			return;
		}
		LinkEntry linkEntry = lookforHead(formthis);
		if (linkEntry == null || linkEntry.next == null)
			return;
		linkEntry.next.entry.onInactive(this);
	}

	@Override
	public void nextFlow(Object request, Object response, IOutputValve formthis) throws CircuitException {
		if (formthis == null) {
			head.entry.flow(request, response, this);
			return;
		}
		LinkEntry linkEntry = lookforHead(formthis);
		if (linkEntry == null || linkEntry.next == null)
			return;
		linkEntry.next.entry.flow(request, response, this);
	}

	private LinkEntry lookforHead(IOutputValve formthis) {
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
		LinkEntry prev =null;
		while (next.next != null) {
			prev=next;
			next = next.next;
			prev.next=null;
			prev.entry=null;
		}
		this.head=null;
		this.last=null;
		disposed=true;
	}

	class LinkEntry {
		LinkEntry next;
		IOutputValve entry;

		public LinkEntry(IOutputValve entry) {
			this.entry = entry;
		}

	}

	@Override
	public IOutputer handler() {
		return handler;
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
	public boolean isDisposed() {
		return disposed;
	}
	class OPipeline implements IOPipeline {
		OutputPipeline target;

		public OPipeline(OutputPipeline target) {
			this.target = target;
		}

		@Override
		public void nextFlow(Object request, Object response, IOutputValve formthis) throws CircuitException {
			target.nextFlow(request, response, formthis);
		}

		@Override
		public String prop(String name) {
			return target.prop(name);
		}

		@Override
		public void nextOnActive(IOutputValve formthis) throws CircuitException {
			this.target.nextOnActive(formthis);
		}

		@Override
		public void nextOnInactive(IOutputValve formthis) throws CircuitException {
			target.nextOnInactive(formthis);
		}

	}

	class Outputer implements IOutputer {
		OutputPipeline target;

		public Outputer(OutputPipeline outputPipeline) {
			this.target = outputPipeline;
		}

		@Override
		public void send(Object request, Object response) throws CircuitException {
			this.target.headFlow(request, response);
		}
		@Override
		public boolean canCloseablePipeline() {
			if (target.last.entry instanceof IClosable) {
				return true;
			}
			return false;
		}
		@Override
		public void closePipeline() throws CircuitException {
			if (target.last.entry instanceof ICloseableOutputValve) {
				ICloseableOutputValve a = (ICloseableOutputValve) target.last.entry;
				a.close(target);
			}
			releasePipeline();
		}

		@Override
		public void releasePipeline() throws CircuitException {
			target.headOnInactive();
			target.dispose();
		}

	}
}