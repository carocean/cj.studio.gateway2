package cj.test.plugin;

import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceSite;
import cj.studio.gateway.socket.app.IGatewayAppSitePlugin;

@CjService(name="$.studio.gateway.app.plugin",isExoteric=true)
public class MyGatewayAppSitePlugin implements IGatewayAppSitePlugin{
	@CjServiceSite
	IServiceSite site;
	@Override
	public Object getService(String name) {
		if("text".equals(name)) {
			return "......text";
		}
		return site.getService(name);
	}

}
