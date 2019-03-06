package cj.test.website.webview;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.stub.IRest;
import cj.test.stub.ICustomStub;
import cj.test.stub.CustomBO;

@CjService(name = "/backend6")
public class ToBackendWebview6 implements IGatewayAppSiteWayWebView {
	@CjServiceRef(refByName="$.rest")
	IRest rest;
	// 异步接收
	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		ICustomStub user=rest.forRemote("rest://backend/website2/").open(ICustomStub.class);
		CustomBO bo=new CustomBO();
		bo.setName("cj");
		user.saveCustom(33L, 23, bo);
		System.out.println("---"+bo.getName()+" "+bo.getAge());
	}

}
