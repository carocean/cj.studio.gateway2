package cj.test.website.webview;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IOutputChannel;
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.ecm.net.io.MemoryInputChannel;
import cj.studio.ecm.net.io.MemoryOutputChannel;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.socket.pipeline.IOutputSelector;
import cj.studio.gateway.socket.pipeline.IOutputer;

@CjService(name = "/backend3")
public class ToBackendWebview3 implements IGatewayAppSiteWayWebView {

	@CjServiceRef(refByName = "$.output.selector")
	IOutputSelector selector;

	// 异步接收
	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		IOutputer back = selector.select("backend-ws");// 回发

		MemoryInputChannel in = new MemoryInputChannel(8192);
		Frame f1 = new Frame(in, "put /uc/ws/ http/1.1");
		MemoryContentReciever mcr = new MemoryContentReciever();
		f1.content().accept(mcr);
		in.begin(null);
		byte[] b = "name=zhaoxb&type=1&age=10&dept=国务院".getBytes();
		in.done(b, 0, b.length);

		IOutputChannel output=new MemoryOutputChannel();
		Circuit c1=new Circuit(output, "ws/1.0 200 ok");
		back.send(f1, c1);
		
		back.closePipeline();
		
		circuit.content().writeBytes("由WsReciever服务接收到达的消息".getBytes());
	}

	
}
