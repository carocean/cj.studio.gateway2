package cj.test.website;

import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import cj.lns.chip.sos.cube.framework.ICube;
import cj.studio.ecm.Scope;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.http.HttpFrame;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.socket.pipeline.IOutputSelector;
import cj.test.website.bo.BlogBO;
import cj.test.website.bo.UserBO;
import cj.test.website.service.IBlogService;
import cj.test.website.service.IUserService;
import cj.ultimate.gson2.com.google.gson.Gson;

@CjService(name="/",scope=Scope.multiton)
public class Home implements IGatewayAppSiteWayWebView{
	@CjServiceRef(refByName="blogService")
	IBlogService blogService;
	@CjServiceRef
	IUserService userService;
	@CjServiceRef(refByName="$.output.selector")
	IOutputSelector selector;
	@CjServiceRef(refByName="plugin.text")
	String text;
	@CjServiceRef(refByName="plugin.mongodb.cctv.home")
	ICube cube;
	@Override
	public void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		BlogBO blog=new BlogBO();
		blog.setId(frame.hashCode()+"");
		blog.setName("....");
		blogService.saveBlog(blog);
		
		UserBO user=new UserBO();
		user.setId(frame.hashCode()+"");
		user.setName("zhaoxb");
		userService.saveUser(user);
		
		List<UserBO> users=userService.query();
		System.out.println("----users--"+users.size());
		
		System.out.println("--------cube is "+cube);
		
		System.out.println("....home:"+frame);
		Document doc=resource.html("/index.html");
		
		Element e=doc.select("a.euser").first();
		e.attr("wsurl",String.format("ws://%s%s/websocket",frame.head("Host"), frame.rootPath()));
		
		System.out.println(this+"");
		if(frame instanceof HttpFrame) {
			HttpFrame hf=(HttpFrame)frame;
			hf.session().attribute("sssss","....");
		}
		circuit.content().writeBytes(doc.html().getBytes());
		for(UserBO bo:users) {
			circuit.content().writeBytes((new Gson().toJson(bo)+"<br>").getBytes());
		}
//		circuit.content().beginWait();
//		new Thread(new Runnable() {
//			
//			@Override
//			public void run() {
				for(int i=0;i<500;i++) {
					circuit.content().writeBytes(("推动金融业高质量发展 习近平这样部署"+"<br>").getBytes());
				}
				
//			}
//		}).start();
//		circuit.content().waitClose();
	}
	
}
