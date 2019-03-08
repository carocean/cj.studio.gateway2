package cj.studio.gateway.socket.serverchannel.udt.valve;

import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.ecm.net.io.MemoryInputChannel;
import cj.studio.gateway.socket.cable.wire.reciever.UdtContentReciever;
import cj.studio.gateway.socket.pipeline.IIPipeline;
import cj.studio.gateway.socket.pipeline.IInputValve;
import cj.studio.gateway.socket.pipeline.IValveDisposable;
import io.netty.channel.Channel;
import io.netty.channel.udt.UdtMessage;

public class LastUdtServerChannelInputValve implements IInputValve,IValveDisposable {
	Channel channel;
	public LastUdtServerChannelInputValve(Channel channel) {
		this.channel=channel;
	}

	@Override
	public void onActive(String inputName,IIPipeline pipeline)
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
				throw new CircuitException("503", "UDT仅支持MemoryContentReciever或者内容接收器为空." + frame);
			}
			if (frame.content().revcievedBytes() > 0) {
				b = frame.content().readFully();
			}
		}
		UdtContentReciever tcr = new UdtContentReciever(channel);
		frame.content().accept(tcr);// 不管是否已存在接收器都覆盖掉

		MemoryInputChannel in = new MemoryInputChannel(8192);
		Frame pack = new Frame(in, "frame / gateway/1.0");// 有三种包：frame,content,last。frame包无内容；content和last包有内容无头
		pack.content().accept(new MemoryContentReciever());
		in.begin(null);
		byte[] data = frame.toBytes();
		in.done(data, 0, data.length);

		UdtMessage okmsg = new UdtMessage(pack.toByteBuf());
		channel.writeAndFlush(okmsg);
//		
		if (b != null) {
			tcr.done(b, 0, b.length);
		}
	}

	@Override
	public void onInactive(String inputName, IIPipeline pipeline) throws CircuitException {
	}

	@Override
	public void dispose(boolean isCloseableOutputValve) {
		if (isCloseableOutputValve) {
			channel.close();
		}
	}
}
