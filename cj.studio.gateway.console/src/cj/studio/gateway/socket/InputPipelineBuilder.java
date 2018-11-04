package cj.studio.gateway.socket;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.app.IGatewayAppSiteProgram;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import cj.studio.gateway.socket.pipeline.IInputPipelineBuilder;

public class InputPipelineBuilder implements IInputPipelineBuilder {
	IServiceProvider parent;
	private String name;

	public InputPipelineBuilder(IServiceProvider parent) {
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
		if (input == null) {
			throw new CircuitException("404", "app中无法创建输入管道：" + prog);
		}
		return input;
	}

}
