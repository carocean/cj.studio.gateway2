package cj.studio.gateway.junction;

import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import cj.studio.gateway.socket.pipeline.IOPipeline;
import cj.studio.gateway.socket.util.SocketContants;

public class BackwardJunction  extends Junction{
	String fromWho;
	String fromProtocol;
	String localAddress;
	String remoteAddress;
	String toWho;
	Class<? extends IGatewaySocket> toTargetClazz;
	private String toProtocol;
	public BackwardJunction(String name) {
		super(name);
	}
//{Pipeline-Name=website2, From-Protocol=app, From-Name=website, To-Name=website2}
	/*<pre>
	管道：acde480011220000-130d-00000004-c2e8c898f2e891b5-6c08f583@httpSite
  	网关中流向：backward httpSite->website
  	--------------------
  		网络协议:app
  		创建时间:18-11-06 04:01:38
  		本地地址:/0:0:0:0:0:0:0:1:8080
  		远程地址:/0:0:0:0:0:0:0:1:53702
  		目标类型:class cj.studio.gateway.socket.ws.WebsocketGatewaySocket

	</pre>
	 */
	public void parse(IOPipeline from, IInputPipeline to, IGatewaySocket toTarget) {
		fromWho=from.prop(SocketContants.__pipeline_fromWho);
		fromProtocol=from.prop(SocketContants.__pipeline_fromProtocol);
		toWho=to.prop(SocketContants.__pipeline_toWho);
		toProtocol=to.prop(SocketContants.__pipeline_toProtocol);
		fromWho=from.prop(SocketContants.__pipeline_fromWho);
		fromWho=from.prop(SocketContants.__pipeline_fromWho);
		this.localAddress=(String)toTarget.getService("$.localAddress");
		this.remoteAddress=(String)toTarget.getService("$.remoteAddress");
		this.toTargetClazz=toTarget.getClass();
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
	public String getToProtocol() {
		return toProtocol;
	}

}
