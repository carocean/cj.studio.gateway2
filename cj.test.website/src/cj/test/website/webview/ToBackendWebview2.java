package cj.test.website.webview;

import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.frame.IFeedback;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.socket.pipeline.IOutputSelector;
import cj.studio.gateway.socket.pipeline.IOutputer;
import io.netty.buffer.ByteBuf;

@CjService(name = "/backend2", scope = Scope.multiton)
public class ToBackendWebview2 implements IGatewayAppSiteWayWebView {
	@CjServiceRef(refByName = "$.output.selector")
	IOutputSelector selector;

	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		IOutputer back = selector.select("backend");// 回发
		IOutputer out=selector.select(frame);
		Frame f1 = new Frame("get /uc/ http/1.1");
		Circuit c1 = new Circuit("http/1.1 200 ok", new IFeedback() {

			@Override
			public void write(ByteBuf bb, Circuit c) throws CircuitException {
				System.out.println("----write");
//				if (bb.isReadable()) {
//					c.content().writeBytes(bb);
//				}
			}

			@Override
			public void done(ByteBuf bb, Circuit c) {
				System.out.println("----done");
				if (bb.isReadable()) {
					c.content().writeBytes(bb);
				}
				try {
					back.releasePipeline();
				} catch (CircuitException e) {
					e.printStackTrace();
				}
				circuit.copyFrom(c, true);
			}

			@Override
			public void begin(Circuit c) {
				System.out.println("----begin");

			}
		});
		back.send(f1, c1);
	}

}
