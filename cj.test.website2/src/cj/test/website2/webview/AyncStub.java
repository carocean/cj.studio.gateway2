package cj.test.website2.webview;

import cj.studio.ecm.annotation.CjService;
import cj.studio.gateway.stub.GatewayAppSiteRestStub;
import cj.test.stub.IAsynStub;
@CjService(name="/async/")
public class AyncStub extends GatewayAppSiteRestStub implements IAsynStub{

	@Override
	public void test(String name, String cnt) {
		System.out.println("-----"+name+"----"+cnt);
	}

}
