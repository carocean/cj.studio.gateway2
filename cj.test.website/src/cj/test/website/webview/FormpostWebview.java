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
import cj.studio.gateway.socket.visitor.IHttpVisitorWriter;

@CjService(name = "/formpost/", scope = Scope.multiton)
public class FormpostWebview implements IGatewayAppSiteWayWebView {
	@CjServiceRef(refByName = "$.output.selector")
	IOutputSelector selector;

	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		System.out.println(frame);
		selector.select(circuit).accept(new HttpPostVisitor() {
			@Override
			protected void endvisit(Frame frame, Circuit circuit, IHttpVisitorWriter writer) {
				// TODO Auto-generated method stub
				System.out.println("****************HttpPostFreeVisitor.endVisitor");
				writer.write("<ul>".getBytes());
				for(int i=0;i<100;i++) {
				writer.write("<li>MediaType是application/x-www-form-urlencoded的接口测...-博客园</li>".getBytes());
				}
				writer.write("</ul>".getBytes());
			}

			@Override
			protected IHttpFormChunkDecoder createMultipartFormDecoder(Frame frame, Circuit circuit) {
				// TODO Auto-generated method stub
				return null;
			}

			
			
		});
	}

}
