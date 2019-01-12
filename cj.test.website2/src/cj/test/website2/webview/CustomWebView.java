package cj.test.website2.webview;

import java.math.BigDecimal;

import cj.studio.ecm.annotation.CjService;
import cj.studio.gateway.stub.GatewayAppSiteRestStub;
import cj.test.stub.ICustomStub;
import cj.test.stub.CustomBO;

@CjService(name = "/custom/")
public class CustomWebView extends GatewayAppSiteRestStub implements ICustomStub {

	@Override
	public CustomBO getCustom(String name,int age) {
		CustomBO user = new CustomBO();
		user.setName(name);
		user.setAge(age);
		return user;
	}

	@Override
	public CustomBO find(String name, int age, Long v, String cnt) {
		CustomBO user = new CustomBO();
		user.setName(name);
		user.setAge(age);
		return user;
	}

	@Override
	public void test() {
		System.out.println("------");
		
	}
	@Override
	public int getAge(int max) {
		// TODO Auto-generated method stub
		return max;
	}
	@Override
	public int limit(int max, Float f, BigDecimal bd) {
		float s=f+max;
		return bd.multiply(new BigDecimal(s)).intValue();
	}

	@Override
	public void saveCustom(Long s,int age,CustomBO bo) {
		// TODO Auto-generated method stub
		System.out.println("++++++++"+bo);
	}
}
