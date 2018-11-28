package cj.studio.gateway.conf;

import java.security.Key;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

import cj.ultimate.util.StringUtil;

public class ServerInfo {
	String name;
	String protocol;
	String host;
	String road;
	Map<String, String> props;
	private transient Key jwtkey;

	public ServerInfo() {
		props = new HashMap<>();
	}

	public ServerInfo(String name) {
		this();
		this.name = name;
	}
	public String getRoad() {
		return road;
	}
	public void setRoad(String road) {
		this.road = road;
	}
	public String getHandshakeDelegater() {
		if(props==null)return "";
		String handshakeDelegater = props.get("JWT.handshakeDelegater");
		return handshakeDelegater;
	}
	public String getJWTVerifyTokenDelegater() {//获取验证委托目标
		if(props==null)return "";
		String verifyTokenDelegater = props.get("JWT.verifyTokenDelegater");
		return verifyTokenDelegater;
	}
	public boolean isJWTActived() {
		if(props==null)return false;
		String act = props.get("JWT.actived");
		if(StringUtil.isEmpty(act))return false;
		return Boolean.valueOf(act);
	}
	public Key getJWTKey() {
		if (this.jwtkey != null)
			return jwtkey;
		if(props==null)return null;
		Key key = null;
		String alg = props.get("JWT.algorithm");
		String encodedstr = props.get("JWT.encoded");
		if (StringUtil.isEmpty(alg) || StringUtil.isEmpty(encodedstr))
			return null;
		byte[] encoded = encodedstr.getBytes();
		key = new SecretKeySpec(encoded, alg);
		jwtkey=key;
		return key;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Map<String, String> getProps() {
		return props;
	}

	

}
