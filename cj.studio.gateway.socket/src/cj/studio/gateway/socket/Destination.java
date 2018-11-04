package cj.studio.gateway.socket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Destination {
	String name;
	String token;
	List<String> uris;
	Map<String, String>props;
	public Destination() {
		props=new HashMap<>();
		uris=new ArrayList<>();
	}
	public Destination(String domain) {
		this();
		setName(domain);
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public static String getProtocol(String uri) {
		return uri.substring(0, uri.indexOf("://"));
	}
	public List<String> getUris() {
		return uris;
	}
	public Map<String, String> getProps() {
		return props;
	}
}
