package cj.studio.gateway.mic;

public class MicConfig {
	String location;
	String host;
	long reconnDelay;
	long reconnPeriod;
	String cjtoken;
	public MicConfig() {
		reconnDelay = 5000L;
		reconnPeriod = 10000L;
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

}
