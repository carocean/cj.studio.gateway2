package cj.studio.ecm.net.io;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.IOutputChannel;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class MemoryOutputChannel implements IOutputChannel {
	ByteBuf buf;
	Circuit circuit;
	private long writedBytes;
	private boolean isDone;

	public MemoryOutputChannel() {
		buf = Unpooled.buffer(8192);
	}

	@Override
	public void write(byte[] b, int pos, int length) {
		buf.writeBytes(b, pos, length);
		writedBytes += (length - pos);
	}

	@Override
	public void begin(Circuit circuit) {
		this.circuit = circuit;
		isDone = false;
	}

	@Override
	public void done(byte[] b, int pos, int length) {
		buf.writeBytes(b, pos, length);
		writedBytes += (length - pos);
		isDone = true;
		circuit = null;
	}

	@Override
	public long writedBytes() {
		return writedBytes;
	}

	public byte[] readFully() {
		if (!isDone) {
			throw new EcmException("输出还没完成");
		}
		byte[] b = new byte[buf.readableBytes()];
		buf.readBytes(b, 0, b.length);
		return b;
	}
}
