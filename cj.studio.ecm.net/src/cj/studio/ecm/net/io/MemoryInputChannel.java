package cj.studio.ecm.net.io;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IContentReciever;
import cj.studio.ecm.net.IInputChannel;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class MemoryInputChannel implements IInputChannel {
	ByteBuf buf;
	private IContentReciever reciever;
	private long writedBytes;
	private Frame frame;
	private int capacity;
	boolean isDone;
	public MemoryInputChannel(int capacity) {
		buf = Unpooled.buffer(8192);
		this.capacity = capacity;
	}
	public MemoryInputChannel() {
		this(8192);
	}
	@Override
	public Frame frame() {
		return frame;
	}
	@Override
	public void writeBytes(byte[] b) throws CircuitException{
		writeBytes(b, 0, b.length);
	}

	@Override
	public void writeBytes(byte[] b, int pos, int length) throws CircuitException{
		buf.writeBytes(b, pos, length);
		writedBytes += length - pos;
		if (buf.readableBytes() > capacity) {
			flush();
		}
	}

	@Override
	public Frame begin(Object request) {
		this.frame = (Frame) request;
		isDone=false;
		return frame;
	}

	@Override
	public void done(byte[] b, int pos, int length) throws CircuitException {
		flush();// 把缓存刷入
		if (reciever == null) {
			throw new EcmException("没有reciever");
		}
		reciever.done(b, pos, length);// 最后一个不入缓存
		writedBytes += length - pos;
		isDone=true;
	}
	@Override
	public boolean isDone() {
		return isDone;
	}
	@Override
	public void flush() throws CircuitException{
		if (buf.readableBytes() > 0) {
			byte[] b = new byte[buf.readableBytes()];
			buf.readBytes(b, 0, b.length);
			if (reciever == null) {
				throw new EcmException("没有reciever");
			}
			reciever.recieve(b, 0, b.length);
		}
	}

	@Override
	public long writedBytes() {
		return writedBytes;
	}

	@Override
	public void accept(IContentReciever reciever) {
		if (reciever == this.reciever)
			return;
		reciever.begin(frame);
		this.reciever = reciever;
	}

}
