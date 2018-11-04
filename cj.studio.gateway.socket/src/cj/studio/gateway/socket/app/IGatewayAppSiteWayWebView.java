package cj.studio.gateway.socket.app;

import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.net.web.HttpCircuit;
import cj.studio.ecm.net.web.HttpFrame;

public interface IGatewayAppSiteWayWebView {
	void flow(HttpFrame frame, HttpCircuit circuit,
			IGatewayAppSiteResource resource) throws CircuitException;
}
