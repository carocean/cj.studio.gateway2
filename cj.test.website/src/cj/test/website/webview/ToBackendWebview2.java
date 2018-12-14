package cj.test.website.webview;

import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.ICircuitContent;
import cj.studio.ecm.net.IOutputChannel;
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.ecm.net.io.MemoryInputChannel;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.socket.pipeline.IOutputSelector;
import cj.studio.gateway.socket.pipeline.IOutputer;

@CjService(name = "/backend2", scope = Scope.multiton)
public class ToBackendWebview2 implements IGatewayAppSiteWayWebView {

	@CjServiceRef(refByName = "$.output.selector")
	IOutputSelector selector;

	// 异步接收
	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		IOutputer back = selector.select("backend");// 回发

		MemoryInputChannel in = new MemoryInputChannel(8192);
		Frame f1 = new Frame(in, "post /uc/ http/1.1");
		f1.contentType("application/x-www-form-urlencoded");

		MemoryContentReciever mcr = new MemoryContentReciever();
		f1.content().accept(mcr);
		in.begin(null);
		byte[] b = "name=zhaoxb&type=1&age=10&dept=国务院".getBytes();
		in.done(b, 0, b.length);

		IOutputChannel out = new SyncOutputChannel(circuit);
		Circuit c1 = new Circuit(out, "http/1.1 200 ok");
		
		circuit.content().beginWait();//开始等待
		
		back.send(f1, c1);
		circuit.content().waitClose(100000L);
		System.out.println("----等已完成----");
		back.closePipeline();
	}

	public class SyncOutputChannel implements IOutputChannel {
		ICircuitContent cnt;
		private long writedBytes;
		private Circuit circuit;
		public SyncOutputChannel(Circuit circuit) {
			cnt=circuit.content();
			this.circuit=circuit;
		}

		@Override
		public void write(byte[] b, int pos, int length) {
			cnt.writeBytes(b, pos, length);
			writedBytes+=length-pos;
		}

		@Override
		public void begin(Circuit c) {
			StringBuffer sb=new StringBuffer();
			c.print(sb);
			System.out.println("---begin--"+sb);
			circuit.fillFrom(c);
		}

		@Override
		public void done(byte[] b, int pos, int length) {
			cnt.writeBytes(b, pos, length);
			cnt.close();
			System.out.println("----内容接收已关闭---");
			writedBytes+=length-pos;
		}

		@Override
		public long writedBytes() {
			return writedBytes;
		}

	}
}
