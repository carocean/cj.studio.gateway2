package cj.studio.gateway.socket.app.valve;

import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.socket.pipeline.IIPipeline;
import cj.studio.gateway.socket.pipeline.IInputValve;
import cj.studio.gateway.socket.util.SocketContants;
import cj.ultimate.util.StringUtil;

public class CheckUrlInputValve implements IInputValve {

	@Override
	public void onActive(String inputName,  IIPipeline pipeline)
			throws CircuitException {
		pipeline.nextOnActive(inputName,  this);
	}

	@Override
	public void flow(Object request, Object response, IIPipeline pipeline) throws CircuitException {
		String uri = "";
		if (request instanceof Frame) {
			Frame f = (Frame) request;
			uri = f.path();
		}
		int dotpos = uri.lastIndexOf(".");
		if (dotpos < 0&&"http".equals(pipeline.prop(SocketContants.__pipeline_fromProtocol)) && !uri.endsWith("/")) {// 重定向
			if (response instanceof Circuit) {
				Circuit c = (Circuit) response;
				Frame f = (Frame) request;
				c.status("302");
				c.message("Redirect url.");
				String fullUrl = uri;
				String qstr = f.queryString();
				if (!StringUtil.isEmpty(qstr)) {
					fullUrl = String.format("%s/?%s", uri, qstr);
				}else {
					fullUrl = String.format("%s/", uri);
				}
				c.head("Location", fullUrl);
			} 
			return;
		}
		pipeline.nextFlow(request, response, this);
	}

	@Override
	public void onInactive(String inputName, IIPipeline pipeline) throws CircuitException {
		pipeline.nextOnInactive(inputName, this);
	}

}
