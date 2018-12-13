package cj.test.website.webview;

import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.socket.io.MultipartFormContentReciever;
import cj.studio.gateway.socket.io.decoder.mutipart.IFieldDataListener;
import cj.studio.gateway.socket.io.decoder.mutipart.IFieldInfo;
import cj.studio.gateway.socket.io.decoder.mutipart.IFormData;
import cj.studio.gateway.socket.io.decoder.mutipart.listener.FileListener;

@CjService(name = "/formpostmultipart/", scope = Scope.multiton)
public class Formpostmultipart implements IGatewayAppSiteWayWebView {

	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		frame.content().accept(new MultipartFormContentReciever() {

			@Override
			protected void done(Frame frame, IFormData form) {
				String[] names = form.enumFieldName();
				for (String name : names) {
					IFieldInfo f=form.getFieldInfo(name);
					circuit.content().writeBytes(String.format("%s=%s or %s<br>", name, f.filename(),f.value()).getBytes());
				}
			}

			@Override
			protected IFieldDataListener createFieldDataListener() {
				return new FileListener("/Users/caroceanjofers/test/");
			}
		});
	}

}
