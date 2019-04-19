package cj.studio.gateway.socket.io;

import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;


import cj.studio.ecm.EcmException;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IOutputChannel;
import cj.studio.gateway.server.util.DefaultHttpMineTypeFactory;
import cj.studio.gateway.socket.util.HttpResponseStatusMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;

public class HttpOutputChannel implements IOutputChannel {
	Channel channel;
	private long writedBytes;
	private Frame frame;

	public HttpOutputChannel(Channel channel, Frame frame) {
		this.channel = channel;
		this.frame = frame;
	}

	@Override
	public boolean isClosed() {
		return !channel.isActive();
	}

	@Override
	public void write(byte[] b, int pos, int length) {
		ByteBuf bb = Unpooled.buffer(length - pos);
		bb.writeBytes(b, pos, length);
		DefaultHttpContent cnt = new DefaultHttpContent(bb);
		/* ChannelFuture future = */channel.writeAndFlush(cnt);
//		try {
//			future.await(SocketContants.__channel_write_await_timeout, TimeUnit.MILLISECONDS);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		writedBytes += length - pos;
	}

	@Override
	public void begin(Circuit circuit) {
		if (!channel.isActive()) {
			throw new EcmException("套节字已关闭:" + frame);
		}
		if (frame.containsHead(CONNECTION.toString())) {
			circuit.head(CONNECTION.toString(), frame.head(CONNECTION.toString()));
		}
		int stcode=Integer.valueOf(circuit.status());
		String msg=HttpResponseStatusMapper.containsStateCode(stcode)?HttpResponseStatusMapper.message(stcode):circuit.message();
		HttpResponseStatus st = new HttpResponseStatus(Integer.valueOf(circuit.status()),msg);
		DefaultHttpResponse res = new DefaultHttpResponse(HttpVersion.valueOf(circuit.protocol()), st);

		HttpHeaders headers = res.headers();
		String names[] = circuit.enumHeadName();
		for (String name : names) {
			if ("message".equals(name) || "status".equals(name)) {
				continue;
			}
			String v = circuit.head(name);
			headers.add(name, v);
		}
		boolean close = headers.contains(CONNECTION, HttpHeaders.Values.CLOSE, true)
				|| res.getProtocolVersion().equals(HttpVersion.HTTP_1_0)
						&& !headers.contains(CONNECTION, HttpHeaders.Values.KEEP_ALIVE, true);
		if (!close) {
			res.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		}
//		String cntlen = circuit.head("Content-Length");
//		if (StringUtil.isEmpty(cntlen)) {
		setContentLength(res, circuit.content().writedBytes());
//		} else {
//			setContentLength(res, Long.valueOf(cntlen));
//		}

		String ctypeKey = HttpHeaders.Names.CONTENT_TYPE.toString();
		if (circuit.containsContentType()) {
			res.headers().set(HttpHeaders.Names.CONTENT_TYPE, circuit.contentType());
		} else {
			String extName = frame.url();
			int pos = extName.lastIndexOf(".");
			extName = extName.substring(pos + 1, extName.length());
			if (DefaultHttpMineTypeFactory.containsMime(extName)) {
				headers.add(ctypeKey, DefaultHttpMineTypeFactory.mime(extName));
			} else {
				String mime = "text/html; charset=utf-8";
				res.headers().set(HttpHeaders.Names.CONTENT_TYPE, mime);
			}
		}

		ChannelFuture f = channel.writeAndFlush(res);

		if (close) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}

	@Override
	public void done(byte[] b, int pos, int length) {
		if (!channel.isActive()) {
			throw new EcmException("套节字已关闭:" + frame);
		}
		ByteBuf bb = Unpooled.buffer(length - pos);
		bb.writeBytes(b, pos, length);
		LastHttpContent cnt = new DefaultLastHttpContent(bb);
		/* ChannelFuture future = */ channel.writeAndFlush(cnt);
//		try {
//			future.await(SocketContants.__channel_write_await_timeout, TimeUnit.MILLISECONDS);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		writedBytes += length - pos;
		this.channel = null;
		this.frame = null;
	}

	@Override
	public long writedBytes() {
		return writedBytes;
	}

}
