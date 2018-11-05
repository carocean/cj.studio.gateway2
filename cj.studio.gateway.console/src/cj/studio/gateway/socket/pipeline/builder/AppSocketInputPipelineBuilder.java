package cj.studio.gateway.socket.pipeline.builder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.app.IGatewayAppSiteProgram;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import cj.studio.gateway.socket.pipeline.IInputPipelineBuilder;

public class AppSocketInputPipelineBuilder implements IInputPipelineBuilder {
	IServiceProvider parent;
	private String name;
	private Map<String, String> props;
	public AppSocketInputPipelineBuilder(IServiceProvider parent) {
		this.parent = parent;
	}

	@Override
	public IInputPipelineBuilder name(String name) {
		this.name = name;
		return this;
	}

	@Override
	public IInputPipeline createPipeline() throws CircuitException {
		IGatewayAppSiteProgram prog = ((IGatewayAppSiteProgram) parent.getService("$.app.program"));
		IInputPipeline input = prog.createInputPipeline(name);
		if(props!=null) {
			Set<String> set=props.keySet();
			for(String key:set) {
				input.prop(key, props.get(key));
			}
		}
		if (input == null) {
			throw new CircuitException("404", "app中无法创建输入管道：" + prog);
		}
		return input;
	}

	@Override
	public IInputPipelineBuilder prop(String name, String value) {
		if(props==null) {
			props=new HashMap<>();
		}
		props.put(name, value);
		return this;
	}

}
