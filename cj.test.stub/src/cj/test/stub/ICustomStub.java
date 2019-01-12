package cj.test.stub;

import java.math.BigDecimal;

import cj.studio.gateway.stub.annotation.CjStubInContent;
import cj.studio.gateway.stub.annotation.CjStubInHead;
import cj.studio.gateway.stub.annotation.CjStubInParameter;
import cj.studio.gateway.stub.annotation.CjStubMethod;
import cj.studio.gateway.stub.annotation.CjStubReturn;
import cj.studio.gateway.stub.annotation.CjStubService;

@CjStubService(bindService = "/custom/", usage = "xx服务")
public interface ICustomStub {

	@CjStubMethod(alias = "getCustom", protocol = "http/1.1", command = "get", usage = "xxx")
	@CjStubReturn(usage = "xxx", type = CustomBO.class)
	CustomBO getCustom(@CjStubInParameter(key = "name", usage = "xx") String name,
			@CjStubInParameter(key = "My-Age", usage = "年龄") int age);

	@CjStubMethod(alias = "find", protocol = "http/1.1", command = "post", usage = "xxx")
	@CjStubReturn(usage = "xxx", type = CustomBO.class)
	CustomBO find(@CjStubInParameter(key = "name", usage = "xx") String name,
			@CjStubInParameter(key = "My-Age", usage = "年龄") int age,
			@CjStubInParameter(key = "v", usage = "xx") Long v, @CjStubInContent(usage = "xx") String cnt);

	@CjStubMethod(alias = "test", protocol = "http/1.1", command = "get", usage = "xxx")
	void test();

	@CjStubMethod(alias = "getAge", protocol = "http/1.1", command = "get", usage = "xxx")
	int getAge(@CjStubInHead(key = "max", usage = "xx") int max);

	@CjStubMethod(protocol = "http/1.1", command = "get", usage = "xxx")
	int limit(@CjStubInHead(key = "max", usage = "xx") int max, @CjStubInParameter(key = "f", usage = "xxx") Float f,
			@CjStubInParameter(key = "bd", usage = "xxx") BigDecimal bd);
	@CjStubMethod(protocol = "http/1.1", command = "post", usage = "xxx")
	void saveCustom(@CjStubInContent(usage="xx")CustomBO bo);
}
