package cj.studio.gateway.socket.pipeline;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.Destination;
import cj.ultimate.util.StringUtil;

public class OutputSelector implements IOutputSelector {
	IOutputPipelineBuilder builder;
	OutputPipelineCollection pipelines;
	Destination destination;

	public OutputSelector(IOutputPipelineBuilder builder, IServiceProvider parent) {
		this.builder = builder;
		this.destination = (Destination) parent.getService("$.destination");
	}

	@Override
	public IOutputer select(Frame frame) throws CircuitException {
		String name = frame.head("From-Pipeline");
		if (StringUtil.isEmpty(name)) {
			new CircuitException("404", "不确定来源的侦");
		}
		return select(name);
	}

	@Override
	public IOutputer select(String name) throws CircuitException {
		if (pipelines == null) {
			this.pipelines = new OutputPipelineCollection();
		}
		IOutputPipeline output = pipelines.get(name);
		if (output != null) {
			return output.handler();
		}
		// 下面创建outputline
		output = builder.name(name).service("Output-Pipeline-Col", pipelines).prop("From-Name", destination.getName())
				.prop("From-Protocol", "app").createPipeline();
		// 激活管道
		try {
			output.headOnActive();
		} catch (Exception e) {
			CircuitException ce = CircuitException.search(e);
			if (ce!=null) {
				ce.printStackTrace();
			}
			throw e;
		}
		pipelines.add(name, output);
		return output.handler();
	}

}
