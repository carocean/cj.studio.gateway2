package cj.test.website;

import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.net.web.HttpCircuit;
import cj.studio.ecm.net.web.HttpFrame;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.socket.pipeline.IOutputer;
import cj.studio.gateway.socket.pipeline.IOutputSelector;

@CjService(name="/test/websocket.html",scope=Scope.multiton)
public class TestWebsocket implements IGatewayAppSiteWayWebView{
	
	@CjServiceRef(refByName="$.output.selector")
	IOutputSelector selector;
	@Override
	public void flow(HttpFrame frame, HttpCircuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		System.out.println("..../test/websocket.html:"+frame);
		//注意：ws协议不支持会话，开发可在应用层设计
		IOutputer output=selector.select(frame);
//		output=selector.select("test");
		output.send(frame, circuit);
//		output.releasePipeline();
//		output.closePipeline();
	}
	
}
