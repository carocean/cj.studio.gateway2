package cj.studio.gateway.mic;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import cj.studio.gateway.socket.pipeline.IInputPipelineBuilder;
import cj.studio.gateway.socket.pipeline.IInputValve;
import cj.studio.gateway.socket.pipeline.InputPipeline;
import cj.studio.gateway.socket.util.SocketContants;

public class MicInputPipelineBuilder implements IInputPipelineBuilder {
	private Map<String, Object> props;
	String name;
	private IServiceProvider parent;
	
	public MicInputPipelineBuilder(IServiceProvider parent) {
		this.parent=parent;
		
	}
	@Override
	public IInputPipelineBuilder name(String name) {
		this.name=name;
		return this;
	}

	@Override
	public IInputPipelineBuilder prop(String name, Object value) {
		if(props==null) {
			props=new HashMap<>();
		}
		props.put(name, value);
		return this;
	}

	@Override
	public IInputPipeline createPipeline() throws CircuitException {
		IInputValve first=new FirstMicInputValve();
		IInputValve last=new LastMicInputValve(parent);
		IInputPipeline input=new InputPipeline(first, last);
		
		input.prop(SocketContants.__pipeline_name, name);
		String toWho=(String)parent.getService("$.server.name");
		input.prop(SocketContants.__pipeline_toWho,toWho);
		input.prop(SocketContants.__pipeline_toProtocol, "tcp");
		
		if(props!=null) {
			Set<String> set=props.keySet();
			for(String key:set) {
				input.prop(key, (String)props.get(key));
			}
		}
		return input;
	}

}
