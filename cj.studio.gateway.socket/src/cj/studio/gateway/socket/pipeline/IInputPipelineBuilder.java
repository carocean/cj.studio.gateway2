package cj.studio.gateway.socket.pipeline;

import cj.studio.ecm.graph.CircuitException;
import io.netty.channel.Channel;

public interface IInputPipelineBuilder {

	IInputPipelineBuilder name(String name);


	IInputPipeline createPipeline() throws CircuitException;

}
