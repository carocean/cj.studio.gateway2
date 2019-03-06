package cj.test.website.webview;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IInputChannel;
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.ecm.net.io.MemoryInputChannel;
import cj.studio.ecm.net.io.MemoryOutputChannel;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.socket.pipeline.IOutputSelector;
import cj.studio.gateway.socket.pipeline.IOutputer;

@CjService(name = "/backend12")
public class ToBackendWebview12 implements IGatewayAppSiteWayWebView {
	@CjServiceRef(refByName="$.output.selector")
	IOutputSelector selector;
	// 异步接收
	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
//		ICustomStub user=rest.forRemote("rest://backend/website2/").open(ICustomStub.class);
		IOutputer out=selector.select("/backend/website2/");
		IInputChannel in=new MemoryInputChannel();
		Frame f=new Frame(in,"post /website2/custom/ http/1.1");
		f.content().accept(new MemoryContentReciever());
		f.head("Rest-StubFace","cj.test.stub.ICustomStub");
		f.head("Rest-Command","saveCustom");
		f.parameter("s","2323");
		in.begin(f);
		String cnt="{\"age\":10,\"content\":{}}";
		byte[] b=cnt.getBytes();
		in.done(b, 0, b.length);
		MemoryOutputChannel output = new MemoryOutputChannel();
		Circuit c = new Circuit(output, String.format("http/1.1 200 OK"));
		out.send(f, c);
		System.out.println(".....");
	}

}
