package cj.test.website2;


import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.http.HttpFrame;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;

@CjService(name="/",scope= Scope.multiton)
public class Home implements IGatewayAppSiteWayWebView {
	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		HttpFrame f=(HttpFrame)frame;
		f.session().attribute("test","123");
		circuit.content().writeBytes("<img src='img/bi.png'>".getBytes());
		resource.redirect("/website2/test/session.html",circuit);
	}
	
}
