package cj.test.website.webview;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.stub.IRest;
import cj.test.stub.IUserStub;
import cj.test.stub.UserBO;

@CjService(name = "/backend6")
public class ToBackendWebview6 implements IGatewayAppSiteWayWebView {
	@CjServiceRef(refByName="$.rest")
	IRest rest;
	// 异步接收
	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		IUserStub user=rest.forRemote("rest://backend/website2/").open(IUserStub.class);
		UserBO bo=user.getUser("cj",34);
		System.out.println("---"+bo.getName()+" "+bo.getAge());
	}

}
