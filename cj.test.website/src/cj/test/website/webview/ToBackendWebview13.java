package cj.test.website.webview;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.socket.pipeline.IOutputSelector;
import cj.studio.gateway.socket.pipeline.IOutputer;

@CjService(name = "/backend13")
public class ToBackendWebview13 implements IGatewayAppSiteWayWebView {
	@CjServiceRef(refByName = "$.output.selector")
	IOutputSelector selector;

	// 异步接收
	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		if (!frame.head("From-Protocol").equals("ws") && !frame.head("From-Protocol").equals("tcp")
				&& !frame.head("From-Protocol").equals("udt")) {
			throw new CircuitException("404", "不是ws|tcp|udt请求");
		}
//		ICustomStub user=rest.forRemote("rest://backend/website2/").open(ICustomStub.class);
		IOutputer out = selector.select(frame);
//		out.closePipeline();
		out.releasePipeline();
	}

}
