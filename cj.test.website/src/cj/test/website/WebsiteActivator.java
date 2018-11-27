package cj.test.website;

import cj.studio.ecm.IEntryPointActivator;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.context.IElement;

public class WebsiteActivator implements IEntryPointActivator{

	@Override
	public void activate(IServiceSite site, IElement e) {
		// TODO Auto-generated method stub
		System.out.println("----activate");
	}

	@Override
	public void inactivate(IServiceSite arg0) {
		// TODO Auto-generated method stub
		System.out.println("----inactivate");
	}

}
