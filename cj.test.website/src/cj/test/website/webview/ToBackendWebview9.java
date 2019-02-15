package cj.test.website.webview;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
@CjService(name = "/backend9")
public class ToBackendWebview9 implements IGatewayAppSiteWayWebView {

	@CjStubRef(remote = "rest://backend/website2/", stub = ICustomStub.class)
	ICustomStub user;

	// 异步接收
	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		CustomBO bo2 =new CustomBO();
		bo2.setAge(40);
		bo2.setName("cj");
		bo2.setSex(1);
		Map<String, CustomBO> map=new HashMap<>();
		map.put("xx", bo2);
		List<CustomBO> list=user.save(map, new CustomBO[] {bo2});
		System.out.println(list);
	}

}
