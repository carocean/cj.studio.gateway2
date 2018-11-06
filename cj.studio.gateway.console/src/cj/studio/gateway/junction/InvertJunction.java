package cj.studio.gateway.junction;

import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.pipeline.IInputPipeline;

public class InvertJunction  extends Junction{

	public InvertJunction(String name) {
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
	public void parse(IInputPipeline inputPipeline, IGatewaySocket socket) {
		System.out.println();
		
	}

}
