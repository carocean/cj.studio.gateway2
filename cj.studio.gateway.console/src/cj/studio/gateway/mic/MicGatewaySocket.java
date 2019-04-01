package cj.studio.gateway.mic;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.socket.Destination;
import cj.studio.gateway.socket.IGatewaySocket;

public class MicGatewaySocket implements IGatewaySocket {
	IServiceProvider parent;
	private Destination dest;
	MicInputPipelineBuilder builder;

	public MicGatewaySocket(IServiceProvider parent) {
		this.parent = parent;
		builder = new MicInputPipelineBuilder(this);
	}

	@Override
	public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {
		return parent.getServices(serviceClazz);
	}

	@Override
	public Object getService(String serviceId) {
		if ("$.pipeline.input.builder".equals(serviceId)) {
			return builder;
		}
		if ("$.socket.name".equals(serviceId)) {
			return this.name();
		}
		return parent.getService(serviceId);
	}

	@Override
	public String name() {
		return dest.getName();
	}

	@Override
	public void connect(Destination dest) throws CircuitException {
		this.dest = dest;

	}

	@Override
	public void close() throws CircuitException {
		this.parent = null;
		this.dest = null;
	}

}
