package cj.studio.gateway.socket.io.decoder.mutipart.siever;

import cj.studio.gateway.socket.io.decoder.mutipart.IBucket;
import cj.studio.gateway.socket.io.decoder.mutipart.ISiever;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class ExpectEndFieldBoundarySiever implements ISiever {
	String boundary;
	int end;
	int index;
	ByteBuf cache;
	public ExpectEndFieldBoundarySiever(String boundary) {
		this.boundary=String.format("\r\n--%s", boundary);
		cache=Unpooled.buffer();
	}

	@Override
	public int end() {
		return end;
	}
	@Override
	public byte[] cache() {
		byte[] b=new byte[cache.readableBytes()];
		cache.readBytes(b);
		return b;
	}
	@Override
	public boolean hasCache() {
		return false;//此cache由于内部消耗了所以对外不可见
	}
	@Override
	public void write(byte b, IBucket bucket) {
		if((char)b==boundary.charAt(index)) {
			index++;
			if(index==boundary.length()) {
				end=1;
				index=0;
				return;
			}
			cache.writeByte(b);
			return;
		}
		index=0;
		while(cache.isReadable()) {
			bucket.writeFieldData(cache.readByte());
		}
		bucket.writeFieldData(b);
	}

}
