package cj.studio.gateway.socket.visitor.decoder.mutipart.siever;

import cj.studio.gateway.socket.visitor.decoder.mutipart.IBucket;
import cj.studio.gateway.socket.visitor.decoder.mutipart.ISiever;

public class ExpectTwoHyphenSiever implements ISiever {
	int index;
	byte[] twoHyphen;
	private int end;
	public ExpectTwoHyphenSiever() {
		twoHyphen=new byte[] {(byte)'-',(byte)'-'};
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
		if(b==twoHyphen[index]) {
			index++;
			if(index==2) {
				end=1;
				index=0;
			}
			return;
		}
		//下面过滤掉的两个字节一定是\r\n
		index++;
		if(index==2) {
			end=2;
			index=0;
		}
	}

}
