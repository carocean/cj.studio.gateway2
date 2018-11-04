package cj.test.website;

import org.jsoup.nodes.Document;

import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.net.web.HttpCircuit;
import cj.studio.ecm.net.web.HttpFrame;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;

@CjService(name="/",scope=Scope.multiton)
public class Home implements IGatewayAppSiteWayWebView{

	@Override
	public void flow(HttpFrame frame, HttpCircuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		System.out.println("....home:"+frame);
		Document doc=resource.html("/index.html");
		System.out.println(this+"");
		frame.session().attribute("a","c...");
		circuit.content().writeBytes(doc.html().getBytes());
	}
	
}
