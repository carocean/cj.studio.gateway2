package cj.test.website.webview;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IInputChannel;
import cj.studio.ecm.net.IOutputChannel;
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.ecm.net.io.MemoryOutputChannel;
import cj.studio.ecm.net.io.SimpleInputChannel;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.socket.pipeline.IOutputSelector;
import cj.studio.gateway.socket.pipeline.IOutputer;
import cj.ultimate.util.StringUtil;

@CjService(name = "/backend/tcpInMemory")
public class ToBackendTcp_in_memory implements IGatewayAppSiteWayWebView {

	@CjServiceRef(refByName = "$.output.selector")
	IOutputSelector selector;
	ExecutorService exe;

	public ToBackendTcp_in_memory() {
		exe = Executors.newCachedThreadPool();
	}

	// 异步接收
	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		String fn = frame.parameter("destFileName");
		if (StringUtil.isEmpty(fn)) {
			throw new CircuitException("404", "缺少参数：destFileName");
		}
		for (int i = 0; i < 1; i++) {
			exe.execute(new Runnable() {

				@Override
				public void run() {
					try {
						sendFile(fn);
					} catch (CircuitException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
		}
		circuit.content().writeBytes("由TcpReciever服务接收到达的消息".getBytes());
	}

	private void sendFile(String fn) throws CircuitException {
		IOutputer back = selector.select("backend-tcp");// 回发
		IOutputChannel output = new MemoryOutputChannel();
		Circuit c1 = new Circuit(output, "tcp/1.0 200 ok");

		IInputChannel in = new SimpleInputChannel();
		Frame f1 = new Frame(in, "put /website/tcp/ http/1.1");
		f1.content().accept(new MemoryContentReciever());
		in.begin(f1);
		f1.parameter("destFileName", fn);
		in.writeBytes("醉酒男子抢夺出租车方向盘 被判刑3年\r\n".getBytes());
		in.writeBytes("未成年人保护法修订 相关部门到湖南调研\r\n".getBytes());
		byte[] b = "弑母少年被带离原生活环境 由相关部门管束\r\n".getBytes();
		in.done(b, 0, b.length);
		
		back.send(f1, c1);
		
//		back.closePipeline();
		back.releasePipeline();
	}

}
