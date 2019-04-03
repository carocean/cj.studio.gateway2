package cj.studio.gateway.mic;

import cj.studio.gateway.IMicNode;
import cj.studio.gateway.IMicNodeInfo;

public class MicNode implements IMicNode{
	String guid;
	String title;
	String desc;
	boolean isEnabled;
	IMicNodeInfo info;
	public MicNode(MicRegistry registry) {
		guid=registry.guid;
		title=registry.title;
		desc=registry.desc;
		isEnabled=registry.enabled;
		info=new MicNodeInfo(registry.getMic());
	}

	@Override
	public String guid() {
		return guid;
	}

	@Override
	public String title() {
		return title;
	}

	@Override
	public String desc() {
		return desc;
	}

	@Override
	public boolean isEnabled() {
		return isEnabled;
	}

	@Override
	public IMicNodeInfo info() {
		return info;
	}
	
	class MicNodeInfo implements IMicNodeInfo{
		String location;
		String host;
		long reconnDelay;
		long reconnPeriod;
		String cjtoken;
		public MicNodeInfo(MicConfig mic) {
			this.location=mic.location;
			this.host=mic.getHost();
			this.reconnDelay=mic.reconnDelay;
			this.reconnPeriod=mic.reconnPeriod;
			this.cjtoken=mic.cjtoken;
		}

		@Override
		public String location() {
			return location;
		}

		@Override
		public String host() {
			return host;
		}

		@Override
		public long reconnDelay() {
			return reconnDelay;
		}

		@Override
		public long reconnPeriod() {
			return reconnPeriod;
		}

		@Override
		public String cjtoken() {
			return cjtoken;
		}
		
	}
}
