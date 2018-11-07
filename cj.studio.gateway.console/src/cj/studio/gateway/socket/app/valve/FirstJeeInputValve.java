package cj.studio.gateway.socket.app.valve;

import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.pipeline.IIPipeline;
import cj.studio.gateway.socket.pipeline.IInputValve;

public class FirstJeeInputValve implements IInputValve{

	@Override
	public void onActive(String inputName, Object request, Object response, IIPipeline pipeline)
			throws CircuitException {
		pipeline.nextOnActive(inputName, request, response, this);
		
	}

	@Override
	public void flow(Object request, Object response, IIPipeline pipeline) throws CircuitException {
		//将之转为httpservlet3.0 request规范,githum.com上netty-servlet-bridge
//		FullHttpRequest req=null;
//		httpreq
//		HttpServletRequestWrapper reqs=new HttpServletRequestWrapper(req);
//		pipeline.nextFlow(request, response, this);
		throw new CircuitException("505", "暂不兼容jee，以等将来实现");
	}

	@Override
	public void onInactive(String inputName, IIPipeline pipeline) throws CircuitException {
		pipeline.nextOnInactive(inputName, this);
		
	}

	

}
