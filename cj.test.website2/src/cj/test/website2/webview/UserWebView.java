package cj.test.website2.webview;

import cj.studio.ecm.annotation.CjService;
import cj.studio.gateway.stub.GatewayAppSiteRestStub;
import cj.test.stub.IUserStub;
import cj.test.stub.UserBO;

@CjService(name = "/user/")
public class UserWebView extends GatewayAppSiteRestStub implements IUserStub {

	@Override
	public UserBO getUser(String name,int age) {
		UserBO user = new UserBO();
		user.setName(name);
		user.setAge(age);
		return user;
	}


}
