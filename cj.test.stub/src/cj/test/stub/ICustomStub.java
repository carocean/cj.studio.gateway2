package cj.test.stub;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import cj.studio.gateway.stub.annotation.CjStubInContentKey;
import cj.studio.gateway.stub.annotation.CjStubInHead;
import cj.studio.gateway.stub.annotation.CjStubInParameter;
import cj.studio.gateway.stub.annotation.CjStubMethod;
import cj.studio.gateway.stub.annotation.CjStubReturn;
import cj.studio.gateway.stub.annotation.CjStubService;

@CjStubService(bindService = "/custom/", usage = "xx服务")
public interface ICustomStub {
	@CjStubMethod(usage = "xxx", command = "post")
	@CjStubReturn(type = Vector.class, elementType = CustomBO.class, usage = "xxx")
	List<CustomBO> save(
			@CjStubInContentKey(key = "map", type = TreeMap.class, elementType = { String.class,
					CustomBO.class }, usage = "xx") Map<String, CustomBO> map,
			@CjStubInContentKey(key = "al",elementType = CustomBO.class, usage = "xx") List<CustomBO> al,
			@CjStubInContentKey(key = "key", type = CustomBO[].class, usage = "xx") CustomBO[] key);

	@CjStubMethod(alias = "getCustom", protocol = "http/1.1", command = "get", usage = "xxx")
	@CjStubReturn(usage = "xxx", type = CustomBO.class)
	CustomBO getCustom(@CjStubInParameter(key = "name", usage = "xx") String name,
			@CjStubInParameter(key = "My-Age", usage = "年龄") int age);

	@CjStubMethod(alias = "find", protocol = "http/1.1", command = "post", usage = "xxx")
	@CjStubReturn(usage = "xxx", type = CustomBO.class)
	CustomBO find(@CjStubInParameter(key = "name", usage = "xx") String name,
			@CjStubInParameter(key = "My-Age", usage = "年龄") int age,
			@CjStubInParameter(key = "v", usage = "xx") Long v,
			@CjStubInContentKey(key = "content", usage = "xx") String cnt);

	@CjStubMethod(alias = "test", protocol = "http/1.1", command = "get", usage = "xxx")
	void test();

	@CjStubMethod(alias = "getAge", protocol = "http/1.1", command = "get", usage = "xxx")
	int getAge(@CjStubInHead(key = "max", usage = "xx") int max);

	@CjStubMethod(protocol = "http/1.1", command = "get", usage = "xxx")
	int limit(@CjStubInHead(key = "max", usage = "xx") int max, @CjStubInParameter(key = "f", usage = "xxx") Float f,
			@CjStubInParameter(key = "bd", usage = "xxx") BigDecimal bd);

	@CjStubMethod(protocol = "http/1.1", command = "post", usage = "xxx")
	void saveCustom(@CjStubInParameter(key = "s", usage = "xxx") Long s,
			@CjStubInContentKey(key = "age", usage = "age") int age,
			@CjStubInContentKey(key = "content", usage = "content") CustomBO bo);
}
