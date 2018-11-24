package cj.test.website.webview;

import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.socket.pipeline.IOutputSelector;
import cj.studio.gateway.socket.visitor.HttpPostVisitor;
import cj.studio.gateway.socket.visitor.IHttpFormChunkDecoder;
import cj.studio.gateway.socket.visitor.IHttpWriter;
import cj.studio.gateway.socket.visitor.decoder.MultipartFormChunkDecoder;
import cj.studio.gateway.socket.visitor.decoder.mutipart.IFieldDataListener;
import cj.studio.gateway.socket.visitor.decoder.mutipart.listener.FileListener;

@CjService(name = "/formpostmultipart/", scope = Scope.multiton)
public class Formpostmultipart implements IGatewayAppSiteWayWebView {
	@CjServiceRef(refByName = "$.output.selector")
	IOutputSelector selector;

	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		System.out.println(frame);
		selector.select(circuit).accept(new HttpPostVisitor() {
			@Override
			protected void endvisit(Frame frame, Circuit circuit, IHttpWriter writer) {
				// TODO Auto-generated method stub
//				System.out.println("****************HttpPostFreeVisitor.endVisitor");
				for (int i = 0; i < 10000; i++) {
					writer.write(String.format("<li>application/formpostmultipart的接口测-博客园-%s</li>", i).getBytes());
				}
				writer.write("</ul>".getBytes());
			}

			@Override
			protected IHttpFormChunkDecoder createMultipartFormDecoder(Frame frame, Circuit circuit) {
				// TODO Auto-generated method stub
				return new MultipartFormChunkDecoder(frame, circuit) {
					@Override
					protected void done(Frame frame, Circuit circuit, IHttpWriter writer) {
						writer.write("<ul>".getBytes());
						String arr[] = frame.enumParameterName();
						for (String key : arr) {
							writer.write(String.format("<li>%s=%s</li>", key, frame.parameter(key)).getBytes());
						}
					}

					@Override
					protected IFieldDataListener createFieldDataListener() {
						return new FileListener("/Users/caroceanjofers/test", 0);
					}
				};
			}

		});
	}

}
