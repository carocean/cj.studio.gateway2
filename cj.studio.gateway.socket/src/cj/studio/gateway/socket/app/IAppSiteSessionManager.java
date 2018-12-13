package cj.studio.gateway.socket.app;

import java.util.List;

import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.session.ISessionEvent;

//生成、管理会话
public interface IAppSiteSessionManager {
	List<ISessionEvent> getEvents();
	boolean checkSession(Object request, Object response, boolean isDoc) throws CircuitException ;
	//定时检测
	void start();
	void stop();
	void wrapCookie(Object request, Object response) throws CircuitException ;
}
