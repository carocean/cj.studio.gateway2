package cj.studio.gateway.socket.visitor.decoder.mutipart.siever;

import cj.studio.gateway.socket.visitor.decoder.mutipart.IBucket;
import cj.studio.gateway.socket.visitor.decoder.mutipart.ISiever;

public class BeginBoundarySiever implements ISiever {
	String boundary;
	int end;
	int index;
	public BeginBoundarySiever(String boundary) {
		this.boundary=String.format("--%s", boundary);
	}

	@Override
	public int end() {
		return end;
	}
	@Override
	public byte[] cache() {
		return null;
	}
	@Override
	public boolean hasCache() {
		return false;
	}
	@Override
	public void write(byte b, IBucket bucket) {
		if((char)b==boundary.charAt(index)) {
			index++;
			if(index==boundary.length()) {
				end=1;
				index=0;
			}
			return;
		}
		index=0;
		throw new RuntimeException("边界无匹配");
	}

}
