package cj.test.website.ws;

import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.http.HttpFrame;
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.ecm.net.io.MemoryInputChannel;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;

@CjService(name="/test/websocket.html",scope=Scope.multiton)
public class TestWebsocket implements IGatewayAppSiteWayWebView{
	
	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		System.out.println("..../test/websocket.html:"+frame);
		
		if(frame instanceof HttpFrame) {
			HttpFrame hf=(HttpFrame)frame;
			System.out.println("session :"+hf.session());
		}
		
		MemoryInputChannel input=new MemoryInputChannel(8192);
		Frame f1=new Frame(input,"put /ss/bb.txt g/1.0");
		MemoryContentReciever r=new MemoryContentReciever();
		f1.content().accept(r);
		input.begin(f1);
		
		if(frame.content().isAllInMemory()) {
			byte[] data=frame.content().readFully();
			System.out.println("----TestWebsocket reciever--"+new String(data));
			input.writeBytes(data);
		}
		
		for(int i=0;i<10;i++) {
			byte[] b=("\r\n......"+i).getBytes();
			input.writeBytes(b);
		}
		
		byte[] b=new byte[0];
		input.done(b, 0, b.length);
		
		sendFrame(f1,circuit);

	}
	private void sendFrame(Frame f1,Circuit circuit) throws CircuitException{
		byte[] b=f1.toBytes();
		circuit.content().writeBytes(b);
	}
	
}
