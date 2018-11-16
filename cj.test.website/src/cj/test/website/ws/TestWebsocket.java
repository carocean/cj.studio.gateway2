package cj.test.website.ws;

import org.jsoup.nodes.Document;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.net.web.HttpFrame;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.socket.pipeline.IOutputSelector;
import cj.studio.gateway.socket.pipeline.IOutputer;

@CjService(name="/test/websocket.html",scope=Scope.multiton)
public class TestWebsocket implements IGatewayAppSiteWayWebView{
	
	@CjServiceRef(refByName="$.output.selector")
	IOutputSelector selector;
	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		System.out.println("..../test/websocket.html:"+frame);
		Document doc=resource.html("/index.html");
		if(frame instanceof HttpFrame) {
			HttpFrame hf=(HttpFrame)frame;
			System.out.println("session :"+hf.session());
		}
		//注意：ws协议不支持会话，开发可在应用层设计
		IOutputer back=selector.select(frame);//回发
		Frame f1=new Frame("put /ss/bb.txt g/1.0");
		f1.content().writeBytes("....ssldld".getBytes());
		Circuit c1=new Circuit("g/1.0 200 ok");
		back.send(f1, c1);
		back.closePipeline();
//		back.releasePipeline();
		IOutputer output=selector.select("website2");
		Frame f=new Frame(String.format("put /website2/ http/1.1"));
		f.content().writeBytes(doc.html().getBytes());
		Circuit c=new Circuit("g/1.0 200 ok");
		try {
			output.send(f, c);
		} catch (CircuitException e) {
			e.printStackTrace();
			throw new EcmException(e);
		}
		output.releasePipeline();
//		output.closePipeline();
	}
	
}
