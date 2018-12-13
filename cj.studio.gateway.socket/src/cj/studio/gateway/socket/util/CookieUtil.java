package cj.studio.gateway.socket.util;

import java.util.HashSet;
import java.util.Set;

import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.http.CookieHelper;
import cj.ultimate.util.StringUtil;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.ServerCookieEncoder;

public class CookieUtil {
	public final static String KEY_SESSION="JSESSION";
	public static String jsession(Frame frame) {
		String jsession = "";
		Set<Cookie> set = CookieHelper.cookies(frame);
		if (set == null || set.isEmpty()) {
			return jsession;
		} else {
			for (Cookie c : set) {
				if (KEY_SESSION.equals(c.getName().toUpperCase())) {
					jsession = c.getValue();
					break;
				}
			}

			return jsession;
		}

	}
	private static Set<Cookie> cookies(Circuit circuit) {
		String cookieString = circuit
				.head(HttpHeaders.Names.SET_COOKIE.toString());
		if (StringUtil.isEmpty(cookieString))
			return null;
		Set<Cookie> cookies = CookieDecoder.decode(cookieString);
		return cookies;
	}
	 /**
     * Sets the maximum age of this {@link Cookie} in seconds.
     * If an age of {@code 0} is specified, this {@link Cookie} will be
     * automatically removed by browser because it will expire immediately.
     * If {@link Long#MIN_VALUE} is specified, this {@link Cookie} will be removed when the
     * browser is closed.
     *
     * @param maxAge The maximum age of this {@link Cookie} in seconds
     */
	public static void appendCookie(Circuit circuit, String key, String v,
			long maxAge) {
		Set<Cookie> set = cookies(circuit);
		boolean exists = false;
		String cookieString = "";
		if (set == null) {
			set = new HashSet<Cookie>();
		}
		for (Cookie c : set) {
			if (c.getName().equals(key)) {
				c.setValue(v);
				if (maxAge < 0){
					maxAge=Long.MIN_VALUE;
				}
				c.setMaxAge(maxAge);
				if (StringUtil.isEmpty(c.getPath()))
					c.setPath("/");//路径决定了cookie的共享区间，/表示站点的所有资源均共享同一cookie，如果以资源路径设为cookie路径，则各个资源均有独自的cookie，这会导致请求一个页面时，页面内的资源各自产生新的会话，因此必须设定此值
				exists = true;
				break;
			}
		}

		if (!exists) {
			Cookie c = new DefaultCookie(key, v);
//			c.setDomain("localhost");
			if (maxAge < 0){
				maxAge=Long.MIN_VALUE;
			}
			c.setMaxAge(maxAge);
			if (StringUtil.isEmpty(c.getPath()))
				c.setPath("/");//路径决定了cookie的共享区间，/表示站点的所有资源均共享同一cookie，如果以资源路径设为cookie路径，则各个资源均有独自的cookie，这会导致请求一个页面时，页面内的资源各自产生新的会话，因此必须设定此值
			set.add(c);
//			Cookie c2 = new DefaultCookie("key", v);//永久保持此会话，用于在cjtoken过期后在服务器端判断并退出session，这一招还是在实在没办法时再这么做吧
//			if (maxAge < 0){
//				maxAge=Long.MIN_VALUE;
//			}
//			c2.setMaxAge(maxAge);
//			if (StringUtil.isEmpty(c2.getPath()))
//				c2.setPath("/");//路径决定了cookie的共享区间，/表示站点的所有资源均共享同一cookie，如果以资源路径设为cookie路径，则各个资源均有独自的cookie，这会导致请求一个页面时，页面内的资源各自产生新的会话，因此必须设定此值
//			set.add(c2);
		}
		for (Cookie c : set) {
			cookieString += ServerCookieEncoder.encode(c) + ";";
		}
		circuit.head(HttpHeaders.Names.SET_COOKIE.toString(), cookieString);
	}
}
