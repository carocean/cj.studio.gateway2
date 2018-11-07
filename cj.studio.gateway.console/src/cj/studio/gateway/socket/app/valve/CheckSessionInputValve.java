package cj.studio.gateway.socket.app.valve;

import javax.servlet.http.HttpServletRequest;

import cj.studio.ecm.IChip;
import cj.studio.ecm.IChipInfo;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.net.web.HttpFrame;
import cj.studio.ecm.net.web.WebUtil;
import cj.studio.gateway.socket.app.IAppSiteSessionManager;
import cj.studio.gateway.socket.pipeline.IIPipeline;
import cj.studio.gateway.socket.pipeline.IInputValve;
import cj.studio.gateway.socket.util.SocketContants;
import cj.ultimate.util.StringUtil;

public class CheckSessionInputValve implements IInputValve {
	public final static String SITE_DOCUMENT = "site.document";
	IAppSiteSessionManager sessionManager;
	private boolean isForbiddenSession;
	private String documentType;

	public CheckSessionInputValve(IServiceProvider parent,IServiceProvider app) {
		this.sessionManager = (IAppSiteSessionManager) parent.getService("$.sessionManager");
		IChip chip = (IChip) app.getService(IChip.class.getName());
		IChipInfo info = chip.info();
		documentType = info.getProperty(SITE_DOCUMENT);
		String isStr = info.getProperty("site.session.forbidden");
		if (StringUtil.isEmpty(isStr)) {
			isStr = "true";
		}
		this.isForbiddenSession = Boolean.valueOf(isStr);
	}

	@Override
	public void onActive(String inputName, Object request, Object response, IIPipeline pipeline)
			throws CircuitException {
		pipeline.nextOnActive(inputName, request, response, this);
	}

	@Override
	public void flow(Object request, Object response, IIPipeline pipeline) throws CircuitException {
		if (isForbiddenSession) {
			pipeline.nextFlow(request, response, this);
			return;
		}
		if(!"http".equals(pipeline.prop(SocketContants.__frame_fromProtocol))) {//如果是ws协议则不分配会话，如果开发者想维护状态可在应用层自设。
			pipeline.nextFlow(request, response, this);
			return;
		}
		boolean isDoc = false;
		if(request instanceof HttpFrame) {
			Frame frame=(Frame)request;
			isDoc=WebUtil.documentMatch(frame.path(), this.documentType);
		}else if(request instanceof HttpServletRequest) {
			//暂不实现
		}
		
		if (sessionManager.checkSession(request, response, isDoc)) {
			wrapSession(request, response);
		}
		pipeline.nextFlow(request, response, this);
	}

	private void wrapSession(Object request, Object response) throws CircuitException {
		if (request instanceof HttpFrame) {
			Frame frame = (Frame) request;
			Circuit circuit = (Circuit) response;
			sessionManager.wrapCookie(frame, circuit);
		} else if (request instanceof HttpServletRequest) {// jee将来实现

		} else {// 什么都不做

		}

	}

	@Override
	public void onInactive(String inputName, IIPipeline pipeline) throws CircuitException {
		pipeline.nextOnInactive(inputName, this);
	}

}
