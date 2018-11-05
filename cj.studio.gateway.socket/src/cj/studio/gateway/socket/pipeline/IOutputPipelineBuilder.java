package cj.studio.gateway.socket.pipeline;

import cj.studio.ecm.graph.CircuitException;

public interface IOutputPipelineBuilder {

	IOutputPipelineBuilder name(String name);
	IOutputPipelineBuilder prop(String name,String value);

	IOutputPipeline createPipeline() throws CircuitException;

}
