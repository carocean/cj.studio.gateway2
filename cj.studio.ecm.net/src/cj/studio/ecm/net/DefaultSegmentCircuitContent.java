package cj.studio.ecm.net;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.ecm.net.io.MemoryInputChannel;
import io.netty.buffer.ByteBuf;

public class DefaultSegmentCircuitContent extends DefaultCircuitContent implements ISegmentCircuitContent {
	int state;// 1createdFrist;2isWritedFirst;0done

	public DefaultSegmentCircuitContent(Circuit owner, IOutputChannel output, ByteBuf buf, int capacity) {
		super(owner, output, buf, capacity);
	}

	public DefaultSegmentCircuitContent(Circuit owner, IOutputChannel writer, ByteBuf buf) {
		super(owner, writer, buf);
	}

	@Override
	public Frame createFirst(String frame_line) throws CircuitException {
		MemoryInputChannel infirst = new MemoryInputChannel(8192);
		Frame first = new Frame(infirst, frame_line);
		first.content().accept(new MemoryContentReciever());
		infirst.begin(first);
		infirst.done(new byte[0], 0, 0);
		state = 1;
		return first;
	}

	@Override
	public void writeBytes(byte[] b, int pos, int len) {
		if (state < 1) {
			throw new EcmException("没有创建first，请转换为ISegmentCircuitContent并调用其方法createFirst.");
		}
		if (state < 2) {// 写入头
			try {
				writeFirst(b, pos, len);
				state = 2;
			} catch (CircuitException e) {
				throw new EcmException(e);
			}
			return;
		}
		// 写入内容
		try {
			writeContent(b, pos, len);
		} catch (CircuitException e) {
			throw new EcmException(e);
		}
	}

	protected void writeFirst(byte[] b, int pos, int len) throws CircuitException {
		MemoryInputChannel inpack = new MemoryInputChannel();
		Frame pack = new Frame(inpack, "frame / gateway/1.0");
		pack.content().accept(new MemoryContentReciever());
		inpack.begin(pack);
		inpack.done(b, 0, b.length);
		byte[] raw = pack.toBytes();
		super.writeBytes(raw, 0, raw.length);
		super.flush();
	}

	protected void writeContent(byte[] b, int pos, int len) throws CircuitException {
		MemoryInputChannel incnt = new MemoryInputChannel(8192);
		Frame cnt = new Frame(incnt, "content / gateway/1.0");
		cnt.content().accept(new MemoryContentReciever());
		incnt.begin(cnt);
		incnt.done(b, pos, len);
		byte[] raw = cnt.toBytes();
		super.writeBytes(raw, 0, raw.length);
		super.flush();
	}

	@Override
	public void done(byte[] b, int pos, int length) throws CircuitException {
		// 写入last
		if (state < 1) {
			throw new EcmException("没有创建first，请转换为ISegmentCircuitContent并调用其方法createFirst.");
		}
		MemoryInputChannel inlast = new MemoryInputChannel(8192);
		Frame last = new Frame(inlast, "last / gateway/1.0");
		last.content().accept(new MemoryContentReciever());
		inlast.begin(last);
		inlast.done(b, pos, length);
		byte[] raw = last.toBytes();
		super.writeBytes(raw, 0, raw.length);
		super.flush();
		this.state = 0;
	}

}
