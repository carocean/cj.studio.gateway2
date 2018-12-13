package cj.studio.gateway.socket.app.valve;

import java.util.Map;

import cj.studio.ecm.IChip;
import cj.studio.ecm.IChipInfo;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.http.HttpCircuit;
import cj.studio.ecm.net.http.HttpFrame;
import cj.studio.ecm.net.util.WebUtil;
import cj.studio.gateway.socket.pipeline.IIPipeline;
import cj.studio.gateway.socket.pipeline.IInputValve;
import cj.ultimate.util.StringUtil;

public class CheckErrorInputVavle implements IInputValve {
	Map<String, String> errors;
	private String documentType;
	public final static String SITE_DOCUMENT = "site.document";

	@SuppressWarnings("unchecked")
	public CheckErrorInputVavle(IServiceProvider parent) {
		errors = (Map<String, String>) parent.getService("$.app.errors");
		IChip chip = (IChip) parent.getService(IChip.class.getName());
		IChipInfo info = chip.info();
		documentType = info.getProperty(SITE_DOCUMENT);
	}

	@Override
	public void onActive(String inputName, IIPipeline pipeline) throws CircuitException {
		pipeline.nextOnActive(inputName, this);
	}

	@Override
	public void flow(Object request, Object response, IIPipeline pipeline) throws CircuitException {
		if (response instanceof HttpFrame) {
			flowWay((HttpFrame) request, (HttpCircuit) response, pipeline);
		} else {
			pipeline.nextFlow(request, response, this);
		}
	}

	private void flowWay(HttpFrame f, HttpCircuit c, IIPipeline pipeline) throws CircuitException {

		try {
			pipeline.nextFlow(f, c, this);
		} catch (Exception e) {
			CircuitException ce = CircuitException.search(e);
			if (ce == null) {
				ce = new CircuitException("503", e);
			}
			boolean isDoc = WebUtil.documentMatch(f.path(), documentType);
			if (isDoc) {
				String page = errors.get(ce.getStatus());
				if (!StringUtil.isEmpty(page)) {
					e.printStackTrace();
					String old = f.url();
					String rootName = f.rootName();
					f.url(String.format("/%s%s?onerror=%s", rootName, page, old));
					try {
						c.cause(ce.messageCause());
						pipeline.nextFlow(f, c, this);
					} catch (Throwable e2) {
						throw e2;
					}
					return;
				}
			}
			throw ce;
		}

	}

	@Override
	public void onInactive(String inputName, IIPipeline pipeline) throws CircuitException {
		pipeline.nextOnInactive(inputName, this);
	}

}
