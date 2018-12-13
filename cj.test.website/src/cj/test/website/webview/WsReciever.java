package cj.test.website.webview;

import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;

@CjService(name="/ws/reciever",scope=Scope.multiton)
public class WsReciever implements IGatewayAppSiteWayWebView{
	
	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		System.out.println("----接收到远端ws发来的消息:"+frame);
		byte[] b=frame.content().readFully();
		System.out.println(new String(b));
	}
	
}
