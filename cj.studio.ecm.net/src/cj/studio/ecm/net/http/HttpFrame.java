package cj.studio.ecm.net.http;

import io.netty.handler.codec.http.Cookie;

import java.util.HashSet;
import java.util.Set;

import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IInputChannel;
import cj.studio.ecm.net.session.ISession;

public class HttpFrame extends Frame {
	ISession session;
	
	public HttpFrame(IInputChannel writer,byte[] frameRaw) throws CircuitException{
		super(writer,frameRaw);
	}

	public HttpFrame(IInputChannel writer,String frame_line) {
		super(writer,frame_line);
	}

	public HttpFrame(IInputChannel writer,String frameline, ISession session2) {
		super(writer,frameline);
		this.session=session2;
	}

	public void setSession(ISession session){
		this.session=session;
	}
	public Set<Cookie> cookie(String key){
		Set<Cookie> set=CookieHelper.cookies(this);
		return set==null?new HashSet<Cookie>():set;
	}
	public ISession session(){
		return session;
	}
}
