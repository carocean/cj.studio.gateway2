package cj.studio.gateway.socket.valve;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.net.web.HttpCircuit;
import cj.studio.ecm.net.web.HttpFrame;
import cj.studio.gateway.socket.pipeline.IIPipeline;
import cj.studio.gateway.socket.pipeline.IInputValve;
import cj.ultimate.util.StringUtil;

public class CheckErrorInputVavle implements IInputValve {
	Map<String, String> errors;

	@SuppressWarnings("unchecked")
	public CheckErrorInputVavle(IServiceProvider parent) {
		errors = (Map<String, String>) parent.getService("$.app.errors");
	}

	@Override
	public void onActive(String inputName, Object request, Object response, IIPipeline pipeline)
			throws CircuitException {
		pipeline.nextOnActive(inputName, request, response, this);
	}

	@Override
	public void flow(Object request, Object response, IIPipeline pipeline) throws CircuitException {
		if (response instanceof HttpCircuit) {
			flowWay((HttpFrame) request, (HttpCircuit) response, pipeline);
		} else if (response instanceof HttpServletResponse) {
			flowJee((HttpServletRequest) request, (HttpServletResponse) response, pipeline);
		} else {
			pipeline.nextFlow(request, response, this);
		}
	}

	private void flowJee(HttpServletRequest request, HttpServletResponse response, IIPipeline pipeline)
			throws CircuitException {
		pipeline.nextFlow(request, response, this);

	}

	private void flowWay(HttpFrame f, HttpCircuit c, IIPipeline pipeline) throws CircuitException {

		try {
			pipeline.nextFlow(f, c, this);
		} catch (Throwable e) {
			CircuitException ce = CircuitException.search(e);
			if (ce != null) {
				String page = errors.get(ce.getStatus());
				if (!StringUtil.isEmpty(page)) {
					String old = f.url();
					String rootName = f.rootName();
					f.url(String.format("/%s%s?onerror=%s", rootName, page, old));
					try {
						pipeline.nextFlow(f, c, this);
					} catch (Throwable e2) {
						throw e2;
					}
					return;
				}
				throw ce;
			}
			throw e;
		}

	}

	@Override
	public void onInactive(String inputName, IIPipeline pipeline) throws CircuitException {
		pipeline.nextOnInactive(inputName, this);
	}

}
