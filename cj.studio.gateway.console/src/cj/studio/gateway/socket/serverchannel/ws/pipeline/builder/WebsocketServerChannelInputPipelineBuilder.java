package cj.studio.gateway.socket.serverchannel.ws.pipeline.builder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import cj.studio.gateway.socket.pipeline.IInputPipelineBuilder;
import cj.studio.gateway.socket.pipeline.IInputValve;
import cj.studio.gateway.socket.pipeline.InputPipeline;
import cj.studio.gateway.socket.serverchannel.ws.valve.FirstWebsocketServerChannelInputValve;
import cj.studio.gateway.socket.serverchannel.ws.valve.LastWebsocketServerChannelInputValve;
import cj.studio.gateway.socket.util.SocketContants;
import io.netty.channel.Channel;

public class WebsocketServerChannelInputPipelineBuilder implements IInputPipelineBuilder {
	private Map<String, String> props;
	private Channel channel;
	String name;
	private IServiceProvider parent;
	
	public WebsocketServerChannelInputPipelineBuilder(IServiceProvider parent, Channel channel) {
		this.channel=channel;
		this.parent=parent;
		
	}

	@Override
	public IInputPipelineBuilder name(String name) {
		this.name=name;
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
		IInputValve first=new FirstWebsocketServerChannelInputValve();
		IInputValve last=new LastWebsocketServerChannelInputValve(channel);
		IInputPipeline input=new InputPipeline(first, last);
		
		input.prop(SocketContants.__pipeline_name, name);
		String toWho=(String)parent.getService("$.server.name");
		input.prop(SocketContants.__pipeline_toWho,toWho);
		input.prop(SocketContants.__pipeline_toProtocol, "ws");
		
		if(props!=null) {
			Set<String> set=props.keySet();
			for(String key:set) {
				input.prop(key, props.get(key));
			}
		}
		return input;
	}

}
