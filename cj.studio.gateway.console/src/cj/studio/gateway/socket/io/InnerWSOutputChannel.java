package cj.studio.gateway.socket.io;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IOutputChannel;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public class InnerWSOutputChannel implements IOutputChannel {
	Channel channel;
	private long writedBytes;
	private Frame frame;

	public InnerWSOutputChannel(Channel channel, Frame frame) {
		this.channel = channel;
		this.frame = frame;
	}
	@Override
	public boolean isClosed() {
		return !channel.isActive();
	}
	@Override
	public void write(byte[] b, int pos, int length) {
		TextWebSocketFrame binf = new TextWebSocketFrame();
		binf.content().writeBytes(b, 0, length);
		channel.writeAndFlush(binf);
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
		TextWebSocketFrame binf = new TextWebSocketFrame();
		binf.content().writeBytes(b, 0, length);
		channel.writeAndFlush(binf);
		writedBytes += length - pos;
		this.channel = null;
		this.frame = null;
	}

	@Override
	public long writedBytes() {
		return writedBytes;
	}

}
