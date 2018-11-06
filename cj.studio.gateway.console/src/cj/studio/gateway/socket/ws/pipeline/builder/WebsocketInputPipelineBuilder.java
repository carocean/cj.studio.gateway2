package cj.studio.gateway.socket.ws.pipeline.builder;

import java.util.HashMap;
import java.util.Map;

import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import cj.studio.gateway.socket.pipeline.IInputPipelineBuilder;
import cj.studio.gateway.socket.pipeline.IInputValve;
import cj.studio.gateway.socket.pipeline.InputPipeline;
import cj.studio.gateway.socket.ws.valve.FirstWebsocketInputValve;
import cj.studio.gateway.socket.ws.valve.LastWebsocketInputValve;
import io.netty.channel.Channel;

public class WebsocketInputPipelineBuilder implements IInputPipelineBuilder {
	private Map<String, String> props;
	private Channel channel;
	public WebsocketInputPipelineBuilder(Channel channel) {
		this.channel=channel;
	}

	@Override
	public IInputPipelineBuilder name(String name) {
		return this;
	}

	@Override
	public IInputPipelineBuilder prop(String name, String value) {
		if(props==null) {
			props=new HashMap<>();
		}
		props.put(name, value);
		return this;
	}

	@Override
	public IInputPipeline createPipeline() throws CircuitException {
		IInputValve first=new FirstWebsocketInputValve();
		IInputValve last=new LastWebsocketInputValve(channel);
		IInputPipeline input=new InputPipeline(first, last);
		
		return input;
	}

}
