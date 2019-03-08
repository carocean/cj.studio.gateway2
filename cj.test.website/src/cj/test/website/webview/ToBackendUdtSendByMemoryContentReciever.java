package cj.test.website.webview;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IInputChannel;
import cj.studio.ecm.net.IOutputChannel;
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.ecm.net.io.MemoryOutputChannel;
import cj.studio.ecm.net.io.SimpleInputChannel;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.socket.pipeline.IOutputSelector;
import cj.studio.gateway.socket.pipeline.IOutputer;

@CjService(name = "/udt/send/memory/content/reciever")
public class ToBackendUdtSendByMemoryContentReciever implements IGatewayAppSiteWayWebView {

	@CjServiceRef(refByName = "$.output.selector")
	IOutputSelector selector;

	// 异步接收
	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		
		IOutputer back = selector.select("backend-udt");// 回发

		IOutputChannel output=new MemoryOutputChannel();
		Circuit c1=new Circuit(output, "ss/1.0 200 ok");
		
		IInputChannel in = new SimpleInputChannel();
		Frame f1 = new Frame(in, "put /website/udt/ http/1.1");
		in.begin(f1);
		f1.content().accept(new MemoryContentReciever());//
		
		byte[] b="我是中国人".getBytes();
		in.done(b, 0, b.length);//使用MemoryContentReciever接收器放在send前发送
		
		back.send(f1, c1);
		
		
		back.releasePipeline();
		circuit.content().writeBytes("由TcpReciever服务接收到达的消息".getBytes());
	}

	
}
