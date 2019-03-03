package cj.test.website.webview;

import cj.studio.ecm.annotation.CjBridge;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.stub.annotation.CjStubRef;
import cj.test.stub.IAsynStub;
//演示异步调用存根
@CjBridge(aspects = "@rest")
@CjService(name = "/backend11")
public class ToBackendWebview11 implements IGatewayAppSiteWayWebView {

	@CjStubRef(remote = "rest://udt/website2/", stub = IAsynStub.class,async=true)
	IAsynStub user;

	// 异步接收
	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		user.test("cj", "xxx");
	}

}
