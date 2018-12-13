package cj.test.road;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.socket.app.GatewayAppSiteProgram;
import cj.studio.gateway.socket.app.ProgramAdapterType;

@CjService(name = "$.cj.studio.gateway.road", isExoteric = true)
public class WebsiteProgram extends GatewayAppSiteProgram {
	
	@Override
	protected void onstart(cj.studio.gateway.socket.Destination dest, String assembliesHome, ProgramAdapterType type)
			throws CircuitException {
		System.out.println("------road start----");
	}

}
