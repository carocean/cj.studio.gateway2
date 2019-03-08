package cj.studio.gateway.socket.io;

import java.util.concurrent.TimeUnit;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IOutputChannel;
import cj.studio.gateway.socket.util.SocketContants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.udt.UdtMessage;

public class UdtOutputChannel implements IOutputChannel {
	Channel channel;
	private long writedBytes;
	private Frame frame;

	public UdtOutputChannel(Channel channel, Frame frame) {
		this.channel = channel;
		this.frame = frame;
	}
	@Override
	public boolean isClosed() {
		return !channel.isActive();
	}
	@Override
	public void write(byte[] b, int pos, int length) {
		ByteBuf bb=Unpooled.buffer(length-pos);
		bb.writeBytes(b,pos,length);
		UdtMessage msg = new UdtMessage(bb);
		ChannelFuture future =channel.writeAndFlush(msg);
		try {
			future.await(SocketContants.__channel_write_await_timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
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
		ByteBuf bb=Unpooled.buffer(length-pos);
		bb.writeBytes(b,pos,length);
		UdtMessage msg = new UdtMessage(bb);
		ChannelFuture future =channel.writeAndFlush(msg);
		try {
			future.await(SocketContants.__channel_write_await_timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		writedBytes += length - pos;
		this.channel = null;
		this.frame = null;
	}

	@Override
	public long writedBytes() {
		return writedBytes;
	}

}
