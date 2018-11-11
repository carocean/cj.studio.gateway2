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

@CjService(name="/tcp",scope=Scope.multiton)
public class TcpWebview implements IGatewayAppSiteWayWebView{
	@CjServiceRef(refByName="$.output.selector")
	IOutputSelector selector;
	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		IOutputer back=selector.select(frame);//回发
		Frame f1=new Frame("put /ss/bb.txt g/1.0");
		f1.content().writeBytes("....ssldld".getBytes());
		Circuit c1=new Circuit("g/1.0 200 ok");
		back.send(f1, c1);
		back.closePipeline();
//		back.releasePipeline();
	}

}
