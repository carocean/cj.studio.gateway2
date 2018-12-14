package cj.studio.gateway.socket.serverchannel.ws.valve;

import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.socket.pipeline.IIPipeline;
import cj.studio.gateway.socket.pipeline.IInputValve;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public class LastWebsocketServerChannelInputValve implements IInputValve {
	Channel channel;
	public LastWebsocketServerChannelInputValve(Channel channel) {
		this.channel=channel;
	}

	@Override
	public void onActive(String inputName, IIPipeline pipeline)
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
		Frame frame=(Frame)request;
//		BinaryWebSocketFrame binf = new BinaryWebSocketFrame();
		TextWebSocketFrame binf=new TextWebSocketFrame();
		binf.content().writeBytes(frame.toByteBuf());
		channel.writeAndFlush(binf);
	}

	@Override
	public void onInactive(String inputName, IIPipeline pipeline) throws CircuitException {
		channel.close();
	}

}
