package cj.test.website.webview;

import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;

@CjService(name="/formget/",scope=Scope.multiton)
public class FormGetWebview implements IGatewayAppSiteWayWebView{

	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		System.out.println(frame);
	}

}
