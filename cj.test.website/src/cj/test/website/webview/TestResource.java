package cj.test.website.webview;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import org.jsoup.nodes.Document;

@CjService(name = "/test/resource.html")
public class TestResource implements IGatewayAppSiteWayWebView {
    @Override
    public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
        Document doc= resource.html(frame.relativePath());
        circuit.content().writeBytes(doc.toString().getBytes());
    }
}
