package cj.studio.gateway.socket.app;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;

public interface IGatewayAppSiteWayWebView {
	void flow(Frame frame, Circuit circuit,
			IGatewayAppSiteResource resource) throws CircuitException;
}
