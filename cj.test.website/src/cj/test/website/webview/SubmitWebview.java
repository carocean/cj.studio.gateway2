package cj.test.website.webview;

import org.jsoup.nodes.Document;

import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;

@CjService(name="/pages/submit.html",scope=Scope.multiton)
public class SubmitWebview implements IGatewayAppSiteWayWebView{

	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		Document doc=resource.html(frame.relativeUrl());
		circuit.content().writeBytes(doc.html().getBytes());
	}

}
