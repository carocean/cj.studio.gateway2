package cj.studio.gateway.socket.app.valve;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.pipeline.IIPipeline;
import cj.studio.gateway.socket.pipeline.IInputValve;
import cj.ultimate.util.StringUtil;

public class CheckUrlInputValve implements IInputValve {

	@Override
	public void onActive(String inputName, Object request, Object response, IIPipeline pipeline)
			throws CircuitException {
		pipeline.nextOnActive(inputName, request, response, this);
	}

	@Override
	public void flow(Object request, Object response, IIPipeline pipeline) throws CircuitException {
		String uri = "";
		if (request instanceof Frame) {
			Frame f = (Frame) request;
			uri = f.path();
		} else {
			HttpServletRequest req = (HttpServletRequest) request;
			uri = req.getRequestURI();
			int pos = uri.lastIndexOf("?");
			if (pos > 0) {
				uri = uri.substring(0, pos);
			}
		}
		int dotpos = uri.lastIndexOf(".");
		if (dotpos < 0 && !uri.endsWith("/")) {// 重定向
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
			} else {
				HttpServletResponse res=(HttpServletResponse)response;
				HttpServletRequest req = (HttpServletRequest) request;
				String fullUrl = uri;
				String qstr = req.getQueryString();
				if (!StringUtil.isEmpty(qstr)) {
					fullUrl = String.format("%s/?%s", uri, qstr);
				}else {
					fullUrl = String.format("%s/", uri);
				}
				res.addHeader("Location", fullUrl);
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
