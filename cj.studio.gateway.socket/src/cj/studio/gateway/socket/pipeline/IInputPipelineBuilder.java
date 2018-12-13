package cj.studio.gateway.socket.pipeline;

import cj.studio.ecm.net.CircuitException;

public interface IInputPipelineBuilder {

	IInputPipelineBuilder name(String name);
	IInputPipelineBuilder prop(String name,Object value);

	IInputPipeline createPipeline() throws CircuitException;

}
