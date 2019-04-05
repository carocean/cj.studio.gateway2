package cj.studio.gateway.socket.serverchannel.tcp.valve;

import java.util.concurrent.TimeUnit;

import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.ecm.net.io.MemoryInputChannel;
import cj.studio.ecm.net.util.TcpFrameBox;
import cj.studio.gateway.socket.cable.wire.reciever.TcpContentReciever;
import cj.studio.gateway.socket.pipeline.IIPipeline;
import cj.studio.gateway.socket.pipeline.IInputValve;
import cj.studio.gateway.socket.pipeline.IValveDisposable;
import cj.studio.gateway.socket.util.SocketContants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

public class LastTcpServerChannelInputValve implements IInputValve,IValveDisposable {
	Channel channel;
	public LastTcpServerChannelInputValve(Channel channel) {
		this.channel=channel;
	}

	@Override
	public void onActive(String inputName,  IIPipeline pipeline)
			throws CircuitException {
		
	}

	@Override
	public void flow(Object request, Object response, IIPipeline pipeline) throws CircuitException {
		if(!(request instanceof Frame) ){
			throw new CircuitException("505", "不支持的请求消息类型:"+request);
		}
		if(!channel.isOpen()) {
			throw new CircuitException("505", "对点网络已关闭，无法处理回推侦："+request);
		}
		Frame frame = (Frame) request;
		byte[] b = null;
		if (frame.content().hasReciever()) {
			if (!frame.content().isAllInMemory()) {
				throw new CircuitException("503", "TCP仅支持MemoryContentReciever或者内容接收器为空." + frame);
			}
			if (frame.content().revcievedBytes() > 0) {
				b = frame.content().readFully();
			}
		}
		TcpContentReciever tcr = new TcpContentReciever(channel);
		frame.content().accept(tcr);// 不管是否已存在接收器都覆盖掉

		MemoryInputChannel in = new MemoryInputChannel(8192);
		Frame pack = new Frame(in, "frame / gateway/1.0");// 有三种包：frame,content,last。frame包无内容；content和last包有内容无头
		pack.content().accept(new MemoryContentReciever());
		in.begin(null);
		byte[] data = frame.toBytes();
		frame.dispose();
		in.done(data, 0, data.length);

		byte[] box = TcpFrameBox.box(pack.toBytes());
		ByteBuf bb = Unpooled.buffer();
		bb.writeBytes(box);
		/* ChannelFuture future = */channel.writeAndFlush(bb);
//		try {
//			future.await(SocketContants.__channel_write_await_timeout, TimeUnit.MILLISECONDS);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		if (b != null) {
			tcr.done(b, 0, b.length);
		}
	}

	@Override
	public void onInactive(String inputName, IIPipeline pipeline) throws CircuitException {
	}
	@Override
	public void dispose(boolean isCloseableOutputValve) {
		if(isCloseableOutputValve) {
			channel.close();
		}
	}
}
