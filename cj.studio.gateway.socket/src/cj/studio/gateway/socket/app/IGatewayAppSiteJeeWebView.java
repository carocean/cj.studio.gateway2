package cj.studio.gateway.socket.app;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cj.studio.ecm.graph.CircuitException;

public interface IGatewayAppSiteJeeWebView {
	void flow(HttpServletRequest request, HttpServletResponse response,
			IGatewayAppSiteResource resource) throws CircuitException;
}
