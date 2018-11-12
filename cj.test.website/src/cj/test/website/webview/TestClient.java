package cj.test.website.webview;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.socket.pipeline.IOutputSelector;
import cj.studio.gateway.socket.pipeline.IOutputer;
import cj.ultimate.util.StringUtil;

@CjService(name = "/test/client/")
public class TestClient implements IGatewayAppSiteWayWebView {
	@CjServiceRef(refByName = "$.output.selector")
	IOutputSelector selector;
	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		IOutputer outer = selector.select("http://news.163.com");// 回发
		String uri=frame.parameter("uri");
		if(StringUtil.isEmpty(uri)) {
			uri="/";
		}
		Frame f1 = new Frame(String.format("%s %s %s",frame.command(),uri,frame.protocol()));
		Circuit c1 = new Circuit("http/1.1 200 ok");
		outer.send(f1, c1);
		circuit.copyFrom(c1, true);
	}

}
