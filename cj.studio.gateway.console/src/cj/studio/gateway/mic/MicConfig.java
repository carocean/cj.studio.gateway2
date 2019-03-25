package cj.studio.gateway.mic;

public class MicConfig {
	String location;
	String host;
	long reconnDelay;
	long reconnPeriod;

	public MicConfig() {
		reconnDelay = 5000L;
		reconnPeriod = 10000L;
	}

	public MicConfig(String location, String host) {
		this();
		this.location = location;
		this.host = host;
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
