package cj.studio.gateway.socket.cable.wire.reciever;

import java.util.concurrent.TimeUnit;

import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IContentReciever;
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.ecm.net.io.MemoryInputChannel;
import cj.studio.gateway.socket.util.SocketContants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.udt.UdtMessage;

public class UdtContentReciever implements IContentReciever {
	Channel channel;
	public UdtContentReciever(Channel channel) {
		this.channel=channel;
	}

	@Override
	public void recieve(byte[] b, int pos, int length) throws CircuitException {
		MemoryInputChannel in=new MemoryInputChannel(8192);
		Frame pack=new Frame(in,"content / gateway/1.0");
		MemoryContentReciever reciever=new MemoryContentReciever();
		pack.content().accept(reciever);
		in.begin(null);
		in.done(b,pos,length);
		
		UdtMessage msg = new UdtMessage(pack.toByteBuf());
		//这种每次都等待写入完成的方式，udt比着tcp慢十倍。
		//但又不能不用此方式，因为在开发者写入后紧接着就调用outputer.closePipeline()方法时，会在nio未真正传输时关闭连接，导致丢数。
		ChannelFuture future =channel.writeAndFlush(msg);
		try {
			future.await(SocketContants.__channel_write_await_timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void done(byte[] b, int pos, int length) throws CircuitException{
		MemoryInputChannel in=new MemoryInputChannel(8192);
		Frame pack=new Frame(in,"last / gateway/1.0");
		MemoryContentReciever reciever=new MemoryContentReciever();
		pack.content().accept(reciever);
		in.begin(null);
		in.done(b,pos,length);
		
		UdtMessage msg = new UdtMessage(pack.toByteBuf());
		ChannelFuture future =channel.writeAndFlush(msg);
		try {
			future.await(SocketContants.__channel_write_await_timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void begin(Frame frame) {
		// TODO Auto-generated method stub

	}

}
