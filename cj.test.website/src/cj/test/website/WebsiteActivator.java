package cj.test.website;

import cj.studio.ecm.IEntryPointActivator;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.context.IElement;
import cj.studio.gateway.IRuntime;
import cj.studio.gateway.socket.Destination;

public class WebsiteActivator implements IEntryPointActivator{

	@Override
	public void activate(IServiceSite site, IElement e) {
		// TODO Auto-generated method stub
		System.out.println("----activate");
		IRuntime runtime=(IRuntime)site.getService("$.gateway.runtime");
		Destination dest=new Destination("news.163.com");
		dest.getUris().add("http://news.163.com");
		runtime.addDestination(dest);
		System.out.println(runtime);
	}

	@Override
	public void inactivate(IServiceSite arg0) {
		// TODO Auto-generated method stub
		System.out.println("----inactivate");
	}

}
