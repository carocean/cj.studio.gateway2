package cj.test.website.webview;

import cj.studio.ecm.annotation.CjBridge;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.examples.backend.usercenter.IUserStub;
import cj.studio.gateway.examples.backend.usercenter.bo.UserBO;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.stub.annotation.CjStubRef;

@CjBridge(aspects = "rest")
@CjService(name = "/backend7")
public class ToBackendWebview7 implements IGatewayAppSiteWayWebView {
	
	@CjStubRef(remote = "rest://backend/uc/", stub = IUserStub.class)
	IUserStub user;

	// 异步接收
	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		UserBO bo = user.getUser("cj", 34);
		System.out.println("---" + bo.getName());
	}

}
