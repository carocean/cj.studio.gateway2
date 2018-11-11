package cj.studio.gateway.socket.serverchannel.tcp.valve;

import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.net.nio.netty.tcp.TcpFrameBox;
import cj.studio.gateway.socket.pipeline.IIPipeline;
import cj.studio.gateway.socket.pipeline.IInputValve;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

public class LastTcpServerChannelInputValve implements IInputValve {
	Channel channel;
	public LastTcpServerChannelInputValve(Channel channel) {
		this.channel=channel;
	}

	@Override
	public void onActive(String inputName, Object request, Object response, IIPipeline pipeline)
			throws CircuitException {
		
	}

	@Override
	public void flow(Object request, Object response, IIPipeline pipeline) throws CircuitException {
		if(!(request instanceof Frame) ){
			throw new CircuitException("505", "不支持的请求消息类型:"+request);
		}
		if(!channel.isWritable()) {
			throw new CircuitException("505", "对点网络已不可写，无法处理回推侦："+request);
		}
		Frame frame=(Frame)request;
		byte[] box = TcpFrameBox.box(frame.toBytes());
		ByteBuf bbuf = Unpooled.directBuffer();
		bbuf.writeBytes(box);
		channel.writeAndFlush(bbuf);
	}

	@Override
	public void onInactive(String inputName, IIPipeline pipeline) throws CircuitException {
		channel.close();
	}

}
