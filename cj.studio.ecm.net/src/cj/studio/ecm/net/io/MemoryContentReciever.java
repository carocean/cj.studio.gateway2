package cj.studio.ecm.net.io;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IContentReciever;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class MemoryContentReciever implements IContentReciever {
	ByteBuf buf;
	boolean isDone;

	public MemoryContentReciever() {
		buf = Unpooled.buffer(8192);
	}

	@Override
	public void begin(Frame frame) {
		isDone=false;
	}

	@Override
	public void recieve(byte[] b, int pos, int length) throws CircuitException{
		buf.writeBytes(b, pos, length);
	}

	@Override
	public void done(byte[] b, int pos, int length) throws CircuitException {
		buf.writeBytes(b, pos, length);
		isDone = true;
	}

	public byte[] readFully() throws CircuitException{
		if (!isDone) {
			throw new EcmException("还没完成");
		}
		if(buf.refCnt()<1)return new byte[0];
		byte[] b = new byte[buf.readableBytes()];
		buf.readBytes(b, 0, b.length);
		if(buf.refCnt()>0) {
			buf.release();
		}
		return b;
	}
}
