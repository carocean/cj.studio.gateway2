package cj.test.website.webview;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.socket.io.XwwwFormUrlencodedContentReciever;

@CjService(name = "/formpost/")
public class FormpostWebview implements IGatewayAppSiteWayWebView {

	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		frame.content().accept(new XwwwFormUrlencodedContentReciever() {
			@Override
			protected void done(Frame frame) {
				String[] names = frame.enumParameterName();
				for (String name : names) {
					circuit.content().writeBytes(String.format("%s=%s<br>", name, frame.parameter(name)).getBytes());
				}
			}
		});
		
	}

}
