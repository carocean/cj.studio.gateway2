package cj.test.website.webview;

import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.socket.pipeline.IOutputSelector;
import cj.studio.gateway.socket.pipeline.IOutputer;

@CjService(name="/backend2",scope=Scope.multiton)
public class ToBackendWebview2 implements IGatewayAppSiteWayWebView{
	@CjServiceRef(refByName="$.output.selector")
	IOutputSelector selector;
	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		IOutputer back=selector.select("backend");//回发
		Frame f1=new Frame("put /uc/ http/1.1");
		Circuit c1=new Circuit("http/1.1 200 ok");
		back.send(f1, c1);
		circuit.copyFrom(c1, true);
//		back.closePipeline();
		back.releasePipeline();
	}

}
