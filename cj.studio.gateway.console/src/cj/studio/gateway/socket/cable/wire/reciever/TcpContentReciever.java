package cj.studio.gateway.socket.cable.wire.reciever;

import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IContentReciever;
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.ecm.net.io.MemoryInputChannel;
import cj.studio.ecm.net.util.TcpFrameBox;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

public class TcpContentReciever implements IContentReciever {
	Channel channel;
	public TcpContentReciever(Channel channel) {
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
		
		byte[] box = TcpFrameBox.box(pack.toBytes());
		ByteBuf bb = Unpooled.buffer();
		bb.writeBytes(box);
		channel.writeAndFlush(bb);
	}

	@Override
	public void done(byte[] b, int pos, int length) throws CircuitException{
		MemoryInputChannel in=new MemoryInputChannel(8192);
		Frame pack=new Frame(in,"last / gateway/1.0");
		MemoryContentReciever reciever=new MemoryContentReciever();
		pack.content().accept(reciever);
		in.begin(null);
		in.done(b,pos,length);
		
		byte[] box = TcpFrameBox.box(pack.toBytes());
		ByteBuf bb = Unpooled.buffer();
		bb.writeBytes(box);
		channel.writeAndFlush(bb);
	}

	@Override
	public void begin(Frame frame) {
		// TODO Auto-generated method stub

	}

}
