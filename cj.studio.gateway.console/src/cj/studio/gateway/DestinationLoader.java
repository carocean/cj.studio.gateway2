package cj.studio.gateway;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.IServiceSetter;
import cj.studio.ecm.Scope;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.Destination;
import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.app.AppGatewaySocket;
import cj.studio.gateway.socket.client.ClientGatewaySocket;

@CjService(name = "dloader", scope = Scope.runtime)
public class DestinationLoader implements IDestinationLoader, IServiceSetter, IServiceProvider {

	private IServiceProvider parent;

	@Override
	public void setService(String arg0, Object service) {
		this.parent = (IServiceProvider) service;

	}

	@Override
	public IGatewaySocket load(Destination dest) throws CircuitException {
		boolean isApp = false;
		String uri = dest.getUris().get(0);
		if (uri.startsWith("app://")) {
			isApp = true;
		}
		IGatewaySocket socket = null;
		if (isApp) {
			socket = new AppGatewaySocket(this);
			socket.connect(dest);
			return socket;
		}
		socket = new ClientGatewaySocket(this);
		socket.connect(dest);
		return socket;
	}

	@Override
	public Object getService(String name) {
		return parent.getService(name);
	}

	@Override
	public <T> ServiceCollection<T> getServices(Class<T> clazz) {
		return parent.getServices(clazz);
	}

}
