package cj.test.website.webview;

import java.io.FileInputStream;
import java.io.IOException;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IInputChannel;
import cj.studio.ecm.net.IOutputChannel;
import cj.studio.ecm.net.io.MemoryOutputChannel;
import cj.studio.ecm.net.io.SimpleInputChannel;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.socket.pipeline.IOutputSelector;
import cj.studio.gateway.socket.pipeline.IOutputer;
import cj.ultimate.util.StringUtil;

@CjService(name = "/backend/tcp")
public class ToBackendTcp implements IGatewayAppSiteWayWebView {

	@CjServiceRef(refByName = "$.output.selector")
	IOutputSelector selector;

	// 异步接收
	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		String fn = frame.parameter("destFileName");
		if (StringUtil.isEmpty(fn)) {
			throw new CircuitException("404", "缺少参数：destFileName");
		}
		IOutputer back = selector.select("backend-tcp");// 回发

		IOutputChannel output = new MemoryOutputChannel();
		Circuit c1 = new Circuit(output, "tcp/1.0 200 ok");

		IInputChannel in = new SimpleInputChannel();
		Frame f1 = new Frame(in, "put /website/tcp/ http/1.1");
		in.begin(f1);
		f1.parameter("destFileName", fn);
		
		back.send(f1, c1);
		
		try {
			FileInputStream fis = new FileInputStream(
//					"/Users/caroceanjofers/Downloads/归档.zip");
					"/Users/caroceanjofers/Downloads/订阅号.png");
			byte[] b = new byte[8192];
			int read = 0;
			while ((read = fis.read(b)) != -1) {
				in.writeBytes(b, 0, read);
			}
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte[] b = new byte[0];
		in.done(b, 0, b.length);
		
//		back.closePipeline();
		back.releasePipeline();
		circuit.content().writeBytes("由TcpReciever服务接收到达的消息".getBytes());
	}

}
