package cj.studio.gateway.socket.pipeline;

import java.util.HashMap;
import java.util.Map;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.graph.CircuitException;
import cj.ultimate.IClosable;

public class OutputPipeline implements IOutputPipeline {
	LinkEntry head;
	LinkEntry last;
	IOPipeline adapter;
	Map<String, String> props;
	Outputer handler;

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
		LinkEntry tmp = head;
		while (tmp != null) {
			tmp = tmp.next;
			tmp.entry = null;
			tmp.next = null;
		}
	}

	class LinkEntry {
		LinkEntry next;
		IOutputValve entry;

		public LinkEntry(IOutputValve entry) {
			this.entry = entry;
		}

	}

	@Override
	public void close() {
		if (!(last instanceof IClosable)) {
			throw new EcmException("输出管道的last valve必须实现接口：IClosable");
		}
		IClosable closable = (IClosable) last;
		closable.close();
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

	class OPipeline implements IOPipeline {
		IOutputPipeline target;

		public OPipeline(IOutputPipeline target) {
			this.target = target;
		}

		@Override
		public void nextFlow(Object request, Object response, IOutputValve formthis) throws CircuitException {
			target.nextFlow(request, response, formthis);
		}

		@Override
		public void close() throws CircuitException {
			target.close();
		}

		@Override
		public String prop(String name) {
			return target.prop(name);
		}

	}

	class Outputer implements IOutputer {
		OutputPipeline target;
		public Outputer(OutputPipeline outputPipeline) {
			this.target=outputPipeline;
		}

		@Override
		public void send(Object request, Object response) throws CircuitException {
			this.target.headFlow(request, response);
		}

		@Override
		public void closePipeline() {
			target.close();
		}

		@Override
		public void releasePipeline() {
			// TODO Auto-generated method stub
			
		}

	}
}