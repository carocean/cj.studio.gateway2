package cj.studio.gateway.mic;

public class MicRegistry {
	String guid;
	String title;
	String desc;
	boolean enabled;
	MicConfig mic;

	public MicRegistry() {
	}

	public MicRegistry(String guid, String title, String location, String host, String desc) {
		super();
		this.guid = guid;
		this.title = title;
		this.desc = desc;
		this.mic = new MicConfig(location, host);
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public MicConfig getMic() {
		return mic;
	}

	public void setMic(MicConfig mic) {
		this.mic = mic;
	}

}
