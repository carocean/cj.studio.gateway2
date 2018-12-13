package cj.studio.gateway.socket.io.decoder.mutipart.siever;

import cj.studio.gateway.socket.io.decoder.mutipart.IBucket;
import cj.studio.gateway.socket.io.decoder.mutipart.ISiever;

public class ContinueCrlfSiever implements ISiever {
	int index;
	byte[] crlf;
	private int end;
	byte[] cached;
	boolean hasCache;
	public ContinueCrlfSiever() {
		crlf = new byte[] { (byte) '\r', (byte) '\n' };
		cached=new byte[2];
	}

	@Override
	public int end() {
		return end;
	}
	@Override
	public byte[] cache() {
		return cached;
	}
	@Override
	public boolean hasCache() {
		return hasCache;
	}
	@Override
	public void write(byte b, IBucket bucket) {
		if (b == crlf[index]) {
			cached[index]=(b);
			index++;
			if (index == 2) {
				end = 1;
				index = 0;
				hasCache=false;
			}
			return;
		}
		cached[index]=(b);
		index++;
		if(index==2) {
			end = 2;// 2表示已结束（>0)且没有匹配
			index=0;//注释掉是因为表示缓冲数
			hasCache=true;
		}
	}

}
