package cj.studio.gateway.junction;

import java.net.SocketAddress;

import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import io.netty.channel.ChannelHandlerContext;

public class ForwardJunction extends Junction {
	String netName;
	String protocol;
	SocketAddress localAddress;
	SocketAddress remoteAddress;
	String destName;
	Class<? extends IGatewaySocket> targetClazz;

	public ForwardJunction(String name) {
		super(name);
	}

	/**
	 * 解析交结点
	 * 
	 * @param line   表示从一端到另一端的连接
	 * @param source 表示连线起点源
	 * @param target 表示连线目标终点
	 */
	public void parse(IInputPipeline line, ChannelHandlerContext source, IGatewaySocket target) {
		this.netName = line.prop("Net-Name");
		this.protocol = line.prop("Protocol");
		this.localAddress = source.channel().localAddress();
		this.remoteAddress = source.channel().remoteAddress();
		this.destName = target.name();
		this.targetClazz = target.getClass();
	}

	public String getNetName() {
		return netName;
	}

	public String getProtocol() {
		return protocol;
	}

	public SocketAddress getLocalAddress() {
		return localAddress;
	}

	public SocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	public String getDestName() {
		return destName;
	}

	public Class<? extends IGatewaySocket> getTargetClazz() {
		return targetClazz;
	}

}
