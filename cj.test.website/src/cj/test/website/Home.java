package cj.test.website;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import cj.lns.chip.sos.cube.framework.ICube;
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
import cj.studio.gateway.socket.visitor.AbstractHttpGetVisitor;
import cj.studio.gateway.socket.visitor.IHttpVisitorWriter;
import cj.test.website.bo.BlogBO;
import cj.test.website.bo.UserBO;
import cj.test.website.service.IBlogService;
import cj.test.website.service.IUserService;

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
		
		selector.select(circuit).accept(new AbstractHttpGetVisitor() {
			String[] content=new String[] {"<html><body><ul>","<li>习近平抵达巴布亚新几内亚 进行国事访问</li>","<li>教师有这些行为要被清出教师队伍</li>","<li>日本网络安全大臣没用过电脑</li>","</ul></body></html>"};
			int index;
			@Override
			public int readChunk(byte[] b, int i, int length) {
				if(index==content.length) {
					return -1;
				}
				String str=content[index];
				byte[] d=null;
				try {
					d = str.getBytes("utf-8");
				} catch (UnsupportedEncodingException e) {
				}
				System.arraycopy(d, 0, b, 0, d.length);
				index++;
				return d.length;
			}

			@Override
			public long getContentLength() {
				int len=0;
				for(int i=0;i<content.length;i++) {
					len+=content[i].getBytes().length;
				}
				return len;
			}

			@Override
			public void close() {
				content=null;
				index=0;
			}
			@Override
			public void beginVisit(Frame frame, Circuit circuit) {
				System.out.println("+++++++HttpPullChunkVisitor.beginVisit++");
				
			}
			@Override
			public void endVisit(IHttpVisitorWriter writer) {
				System.out.println("+++++++HttpPullChunkVisitor.endVisit++");
			}
		});
	}
	
}
