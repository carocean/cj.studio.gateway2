package cj.test.multipart.siever;

import cj.test.multipart.IBucket;
import cj.test.multipart.ISiever;

public class ExpectCrlfSiever implements ISiever {
	int index;
	byte[] crlf;
	private int end;
	public ExpectCrlfSiever() {
		crlf=new byte[] {(byte)'\r',(byte)'\n'};
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
		if(b==crlf[index]) {
			index++;
			if(index==2) {
				end=1;
				index=0;
			}
			return;
		}
		index=0;
		bucket.writeAttributeInfo(b);
	}

}
