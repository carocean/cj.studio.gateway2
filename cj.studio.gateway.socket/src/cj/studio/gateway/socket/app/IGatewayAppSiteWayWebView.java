package cj.studio.gateway.socket.app;

import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;

public interface IGatewayAppSiteWayWebView {
	void flow(Frame frame, Circuit circuit,
			IGatewayAppSiteResource resource) throws CircuitException;
}
