package cj.test.website;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.net.web.HttpFrame;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;

@CjService(name="/",scope=Scope.multiton)
public class Home implements IGatewayAppSiteWayWebView{

	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		System.out.println("....home:"+frame);
		Document doc=resource.html("/index.html");
		Element e=doc.select("a.euser").first();
		e.attr("wsurl",String.format("ws://localhost:8080%s/websocket", frame.rootPath()));
		System.out.println(this+"");
		if(frame instanceof HttpFrame) {
			HttpFrame hf=(HttpFrame)frame;
			hf.session().attribute("sssss","....");
		}
		circuit.content().writeBytes(doc.html().getBytes());
	}
	
}
