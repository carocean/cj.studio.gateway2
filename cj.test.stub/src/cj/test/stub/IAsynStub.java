package cj.test.stub;

import cj.studio.gateway.stub.annotation.CjStubInContentKey;
import cj.studio.gateway.stub.annotation.CjStubInParameter;
import cj.studio.gateway.stub.annotation.CjStubMethod;
import cj.studio.gateway.stub.annotation.CjStubService;

@CjStubService(bindService = "/async/", usage = "xx服务")
public interface IAsynStub {
	@CjStubMethod(command="post",usage = "xx")
	void test(@CjStubInParameter(key = "name", usage = "xx") String name,
			@CjStubInContentKey(key = "cnt", usage = "xx") String cnt);
}
