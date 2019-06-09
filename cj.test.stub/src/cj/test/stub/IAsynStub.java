package cj.test.stub;

import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.stub.annotation.CjStubCircuitStatusMatches;
import cj.studio.gateway.stub.annotation.CjStubInParameter;
import cj.studio.gateway.stub.annotation.CjStubMethod;
import cj.studio.gateway.stub.annotation.CjStubReturn;
import cj.studio.gateway.stub.annotation.CjStubService;

@CjStubService(bindService = "/async/", usage = "xx服务")
public interface IAsynStub {
	@CjStubMethod(command = "get", usage = "xx")
	@CjStubReturn(usage = "xxxx")
	@CjStubCircuitStatusMatches(status = { "200      ok xx", "503 error liao." ,"404 error notfound it."})
	String test(@CjStubInParameter(key = "name", usage = "xx") String name,
			@CjStubInParameter(key = "cnt", usage = "xx") String cnt) throws CircuitException;
}
