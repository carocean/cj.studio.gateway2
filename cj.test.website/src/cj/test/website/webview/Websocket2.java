package cj.test.website.webview;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.socket.pipeline.IOutputSelector;

@CjService(name="/webviews/websocket2.html",scope=Scope.multiton)
public class Websocket2 implements IGatewayAppSiteWayWebView{
	
	@CjServiceRef(refByName="$.output.selector")
	IOutputSelector selector;
	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		System.out.println("..../webviews/websocket2.html:"+frame);
		Document doc=resource.html("/webviews/websocket2.html");
		Element e=doc.select(".ws2").first();
		e.attr("wsurl",String.format("ws://localhost:8081/"));
		
		circuit.content().writeBytes(doc.html().getBytes());
	}
	
}
