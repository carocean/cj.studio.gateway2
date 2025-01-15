package cj.studio.gateway.mic;

import java.util.ArrayList;
import java.util.List;

public class MicConfig {
	String location;
	String host;
	long reconnDelay;
	long reconnPeriod;
	String appId;
	String appKey;
	String appSecret;
	String cjtoken;
	List<String> openportsUrls;
	public MicConfig() {
		reconnDelay = 5000L;
		reconnPeriod = 10000L;
		openportsUrls=new ArrayList<>();
	}

	public MicConfig(String location, String host,String cjtoken) {
		this();
		this.location = location;
		this.host = host;
		this.cjtoken=cjtoken;
	}
	public String getCjtoken() {
		return cjtoken;
	}
	public void setCjtoken(String cjtoken) {
		this.cjtoken = cjtoken;
	}
	public long getReconnDelay() {
		return reconnDelay;
	}

	public void setReconnDelay(long reconnDelay) {
		this.reconnDelay = reconnDelay;
	}

	public long getReconnPeriod() {
		return reconnPeriod;
	}

	public void setReconnPeriod(long reconnPeriod) {
		this.reconnPeriod = reconnPeriod;
	}

	public String getLocation() {
		return location;
	}

	public String getHost() {
		return host;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getAppKey() {
		return appKey;
	}

	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}

	public String getAppSecret() {
		return appSecret;
	}

	public void setAppSecret(String appSecret) {
		this.appSecret = appSecret;
	}

	public List<String> getOpenportsUrls() {
		return openportsUrls;
	}

	public void setOpenportsUrls(List<String> openportsUrls) {
		this.openportsUrls = openportsUrls;
	}
}
