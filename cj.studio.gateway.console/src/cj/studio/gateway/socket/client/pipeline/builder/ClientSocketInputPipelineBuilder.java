package cj.studio.gateway.socket.client.pipeline.builder;

import java.util.HashMap;
import java.util.Map;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.logging.ILogging;
import cj.studio.gateway.socket.client.valve.FirstClientInputValve;
import cj.studio.gateway.socket.client.valve.LastClientInputValve;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import cj.studio.gateway.socket.pipeline.IInputPipelineBuilder;
import cj.studio.gateway.socket.pipeline.IInputValve;
import cj.studio.gateway.socket.pipeline.InputPipeline;
import cj.studio.gateway.socket.util.SocketContants;

public class ClientSocketInputPipelineBuilder implements IInputPipelineBuilder {
	IServiceProvider parent;
	private Map<String, String> props;
	private String name;
	static ILogging logger=CJSystem.logging();
	public ClientSocketInputPipelineBuilder(IServiceProvider parent) {
		this.parent = parent;
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
		String socketName=(String)parent.getService("$.socket.name");
		IInputValve first=new FirstClientInputValve();
		IInputValve last=new LastClientInputValve(parent);
		IInputPipeline input=new InputPipeline(first, last);
		if(props!=null) {
			for(String key:props.keySet()) {
				input.prop(key, props.get(key));
			}
		}
		input.prop(SocketContants.__pipeline_name,name);
		input.prop(SocketContants.__pipeline_toWho,socketName);
		input.prop(SocketContants.__pipeline_toProtocol,"net");
		
		return input;
	}

}
