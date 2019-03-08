package cj.studio.gateway.socket.io;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IOutputChannel;
import cj.studio.ecm.net.util.TcpFrameBox;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

public class TcpOutputChannel implements IOutputChannel {
	Channel channel;
	private long writedBytes;
	private Frame frame;

	public TcpOutputChannel(Channel channel, Frame frame) {
		this.channel = channel;
		this.frame = frame;
	}
	@Override
	public boolean isClosed() {
		return !channel.isActive();
	}
	@Override
	public void write(byte[] b, int pos, int length) {
		byte[] box = TcpFrameBox.box(b,pos,length);
		ByteBuf bb = Unpooled.buffer();
		bb.writeBytes(box);
		channel.writeAndFlush(bb);
		writedBytes += length - pos;
	}

	@Override
	public void begin(Circuit circuit) {
		if (!channel.isActive()) {
			throw new EcmException("套节字已关闭:" + frame);
		}
		
	}
	@Override
	public void done(byte[] b, int pos, int length) {
		if (!channel.isActive()) {
			throw new EcmException("套节字已关闭:" + frame);
		}
		byte[] box = TcpFrameBox.box(b,pos,length);
		ByteBuf bb = Unpooled.buffer();
		bb.writeBytes(box);
		channel.writeAndFlush(bb);
		writedBytes += length - pos;
		this.channel = null;
		this.frame = null;
	}

	@Override
	public long writedBytes() {
		return writedBytes;
	}

}
