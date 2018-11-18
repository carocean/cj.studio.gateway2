package cj.studio.gateway.socket.visitor;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;

public class HttpWriter implements IHttpWriter {
	Channel channel;
	public HttpWriter(Channel channel) {
		this.channel=channel;
	}

	@Override
	public void close() {
		channel=null;
	}

	@Override
	public void write(byte[] b) {
		if(channel.isWritable()) {
			ByteBuf bb=Unpooled.buffer(b.length);
			bb.writeBytes(b);
			write(bb);
		}

	}
	@Override
	public void write(byte[] b,int offset,int len) {
		if(channel.isWritable()) {
			ByteBuf bb=Unpooled.buffer(len-offset);
			bb.writeBytes(b,offset,len);
			write(bb);
		}

	}
	@Override
	public void write(ByteBuf buf) {
		if(channel.isWritable()) {
			HttpContent cnt=new DefaultHttpContent(buf);
			channel.writeAndFlush(cnt);
		}

	}
}
