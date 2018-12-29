package cj.test.website.webview;

import java.math.BigDecimal;

import cj.studio.ecm.annotation.CjBridge;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.stub.annotation.CjStubRef;
import cj.test.stub.CustomBO;
import cj.test.stub.ICustomStub;

@CjBridge(aspects = "@rest")
@CjService(name = "/backend8")
public class ToBackendWebview8 implements IGatewayAppSiteWayWebView {

	@CjStubRef(remote = "rest://backend/website2/", stub = ICustomStub.class)
	ICustomStub user;

	// 异步接收
	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		CustomBO bo = user.find(null, 23, null, null);
		System.out.println("---" + bo);
		user.test();
		int age=user.getAge(100);
		System.out.println(age);
		int l=user.limit(200, 2.01F, new BigDecimal(3));
		System.out.println(l);
	}

}
