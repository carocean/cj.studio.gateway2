package cj.studio.gateway.junction;

import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import cj.studio.gateway.socket.util.SocketContants;
import io.netty.channel.Channel;

public class ForwardJunction extends Junction implements SocketContants {
	String fromWho;
	String fromProtocol;
	String localAddress;
	String remoteAddress;
	String toWho;
	Class<? extends IGatewaySocket> toTargetClazz;
	private String toProtocol;

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
	public void parse(IInputPipeline line, Channel source, IGatewaySocket target) {
		this.fromWho = line.prop(__pipeline_fromWho);
		this.fromProtocol = line.prop(__pipeline_fromProtocol);
		this.toWho = line.prop(__pipeline_toWho);
		this.toProtocol=line.prop(__pipeline_toProtocol);
		this.toTargetClazz = target.getClass();
		this.localAddress = source.localAddress().toString();
		this.remoteAddress = source.remoteAddress().toString();
	}
	public String getToProtocol() {
		return toProtocol;
	}
	public String getFromWho() {
		return fromWho;
	}

	public String getFromProtocol() {
		return fromProtocol;
	}

	public String getLocalAddress() {
		return localAddress;
	}

	public String getRemoteAddress() {
		return remoteAddress;
	}

	public String getToWho() {
		return toWho;
	}

	public Class<? extends IGatewaySocket> getToTargetClazz() {
		return toTargetClazz;
	}

}
