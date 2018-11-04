package cj.studio.gateway.socket.app.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.net.layer.ISession;
import cj.studio.ecm.net.layer.ISessionEvent;
import cj.studio.ecm.net.layer.Session;
import cj.studio.ecm.net.layer.SessionInfo;
import cj.studio.ecm.net.util.IdGenerator;
import cj.studio.ecm.net.web.HttpCircuit;
import cj.studio.ecm.net.web.HttpFrame;
import cj.studio.gateway.socket.app.IAppSiteSessionManager;
import cj.ultimate.util.StringUtil;
/**
 * 应用的会话管理器
 * @author caroceanjofers
 *
 */
public class AppSiteSessionManager implements IAppSiteSessionManager {
	Map<String, ISession> sessions;
	Timer timer;
	long expire;
	List<ISessionEvent> events;
	/**
	 * 会话过期时间，为毫秒。
	 * 
	 * <pre>
	 * -1即永不过期.0为立即过期，默认为-1
	 * </pre>
	 * @param expire
	 */
	public AppSiteSessionManager(long expire) {
		this.expire=expire==-1?Long.MAX_VALUE:expire;
		this.sessions = new ConcurrentHashMap<>();
		this.events=new ArrayList<>();
	}
	@Override
	public List<ISessionEvent> getEvents() {
		return events;
	}

	@Override
	public boolean checkSession(Object request, Object response, boolean isDoc) throws CircuitException {
		if (!isDoc)
			return false;// 不是文档，即是资源，资源无需要生成会话
		if(request instanceof HttpFrame) {
			return checkSessionForWay((Frame)request);
		}else if(request instanceof HttpServletRequest){
			return checkSessionForJee((HttpServletRequest)request);
		}
		return false;
	}

	private boolean checkSessionForJee(HttpServletRequest request) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean checkSessionForWay(Frame request) {
		HttpFrame f = (HttpFrame) request;
		String sid = CookieUtil.jsession(request);
		if (!StringUtil.isEmpty(sid) && sessions.containsKey(sid)) {// 返回
			ISession session = sessions.get(sid);
			session.lastVisitTime(System.currentTimeMillis());
			f.setSession(session);
			return false;
		}
		return true;
	}

	private String genSessionId() {
		String sid = IdGenerator.newInstance().asLongText();
		if (!sessions.containsKey(sid)) {
			return sid;
		}
		return genSessionId();
	}

	@Override
	public void wrapCookie(Object request, Object response) throws CircuitException {
		String sid = genSessionId();
		SessionInfo si = new SessionInfo();
		List<ISessionEvent> events = getEvents();
		if (events == null) {
			events = new ArrayList<>();
		}
		ISession session =createSession(sid, si, events);
		long t = System.currentTimeMillis();
		session.createTime(t);
		session.lastVisitTime(t);
		if(request instanceof HttpFrame) {
			wrapCookieForWay((HttpFrame)request,(HttpCircuit)response,session,sid);
		}else if(request instanceof HttpServletRequest){
			wrapCookieForJee((HttpServletRequest)request,(HttpServletResponse)response,session,sid);
		}
		
	}

	private void wrapCookieForJee(HttpServletRequest request, HttpServletResponse response, ISession session,
			String sid) {
		
	}

	private void wrapCookieForWay(HttpFrame f, HttpCircuit c, ISession session, String sid) {
		f.setSession(session);
		sessions.put(sid, session);
		CookieUtil.appendCookie(c, CookieUtil.KEY_SESSION, sid, Long.MIN_VALUE);// 默认浏览器关闭时过期
	}

	protected ISession createSession(String sid, SessionInfo si, List<ISessionEvent> events) {
		ISession session= new Session(sid, si, events);
		long v= System.currentTimeMillis();
		session.createTime(v);
		session.lastVisitTime(v);
		for(ISessionEvent e:events) {
			e.doEvent("sessionAdded", session);
		}
		return session;
	}

	@Override
	public void start() {
		timer = new Timer();
		java.util.TimerTask task = new MyTimerTask();
		timer.schedule(task, 20000L, 15000L);// 20s后执行，间隔15秒
	}

	@Override
	public void stop() {
		timer.cancel();
		sessions.clear();
	}

	class MyTimerTask extends java.util.TimerTask {

		@Override
		public void run() {
			String[] set=sessions.keySet().toArray(new String[0]);
			for(String key:set){
				ISession s=sessions.get(key);
				if(s==null) {
					continue;
				}
				if(System.currentTimeMillis()-s.lastVisitTime()>expire){
					ISession session=sessions.get(key);
					sessions.remove(key);
					for(ISessionEvent e:events) {
						e.doEvent("sessionRemoved", session);
					}
				}
			}
		}

	}

}
