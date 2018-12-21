package cj.test.stub;

import cj.studio.gateway.stub.annotation.CjStubInParameter;
import cj.studio.gateway.stub.annotation.CjStubMethod;
import cj.studio.gateway.stub.annotation.CjStubReturn;
import cj.studio.gateway.stub.annotation.CjStubService;

@CjStubService(bindService = "/user/", usage = "xx服务")
public interface IUserStub {
	
	@CjStubMethod(alias = "getUser", protocol = "http/1.1", command = "get", usage = "xxx")
	@CjStubReturn(usage = "xxx",type=String.class)
	UserBO getUser(@CjStubInParameter(key = "name", usage = "xx") String name,@CjStubInParameter(key="My-Age",usage="年龄")int age);
}
