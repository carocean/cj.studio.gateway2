package cj.studio.gateway.socket.pipeline;

import cj.studio.ecm.net.CircuitException;

public interface IOutputPipelineBuilder {

	IOutputPipelineBuilder name(String name);
	IOutputPipelineBuilder prop(String name,Object value);

	IOutputPipeline createPipeline() throws CircuitException;

}
