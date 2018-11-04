package cj.studio.gateway.socket.app;

import cj.studio.ecm.IServiceProvider;
import cj.studio.gateway.socket.Destination;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import cj.ultimate.IClosable;

public interface IGatewayAppSiteProgram extends IClosable,IServiceProvider{
	void start(Destination dest, String assembliesHome, ProgramAdapterType type);

	IInputPipeline createInputPipeline(String name);

}
