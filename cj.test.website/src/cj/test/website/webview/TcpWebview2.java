package cj.test.website.webview;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IContentReciever;
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.ecm.net.io.MemoryInputChannel;
import cj.studio.ecm.net.io.MemoryOutputChannel;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.socket.pipeline.IOutputSelector;
import cj.studio.gateway.socket.pipeline.IOutputer;

@CjService(name = "/tcp2", scope = Scope.multiton)
public class TcpWebview2 implements IGatewayAppSiteWayWebView {
	@CjServiceRef(refByName = "$.output.selector")
	IOutputSelector selector;

	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		System.out.println("------tcp---");
//		MemoryContentReciever reciever=new MemoryContentReciever() {
//			@Override
//			public void done(byte[] b, int pos, int length) {
//				super.done(b, pos, length);
//				byte[] data=readFully();
//				System.out.println("-------data is\r\n"+new String(data));
//			}
//		};

		frame.content().accept(new FileContentReciever());

		IOutputer out = selector.select(frame);
		for (int i = 0; i < 10; i++) {
			// 发送头侦,头侦无内容
			MemoryInputChannel in = new MemoryInputChannel();
			Frame f = new Frame(in, "get /website/tcp/reciever/ net/1.0");
			f.head("test", "1323233");
			f.content().accept(new MemoryContentReciever());
			in.begin(f);
			// 发送内容侦，可以发送无限多次内容侦
			for (int j = 0; j < 10; j++) {
				in.writeBytes(String.format("弑母少年被带离原生活环境 由相关部门管束-%s.%s\r\n", i,j).getBytes());
				in.writeBytes("未成年人保护法修订 相关部门到湖南调研\r\n".getBytes());
				in.writeBytes("醉酒男子抢夺出租车方向盘 被判刑3年\r\n".getBytes());
			}
			// 发送结束侦
			byte[] b = "习近平向全国各族各界妇女致以节日祝贺\r\n".getBytes();
			in.done(b, 0, b.length);

			MemoryOutputChannel moc = new MemoryOutputChannel();
			Circuit c = new Circuit(moc, "tcp/1.0 200 OK");

			out.send(f, c);
		}
	}

	class FileContentReciever implements IContentReciever {
		private FileOutputStream out;

		@Override
		public void recieve(byte[] b, int pos, int length) throws CircuitException {
			try {
				out.write(b, pos, length);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void done(byte[] b, int pos, int length) throws CircuitException {
			try {
				out.write(b, pos, length);
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		@Override
		public void begin(Frame frame) {
			try {
				out = new FileOutputStream("/Users/caroceanjofers/" + frame.parameter("destFileName"));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
}
