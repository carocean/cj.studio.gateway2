package cj.studio.gateway.socket.pipeline;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.IAsynchronizer;
import cj.studio.gateway.socket.IChunkVisitor;
import cj.studio.gateway.socket.util.SocketContants;
import cj.ultimate.util.StringUtil;

public class OutputSelector implements IOutputSelector, SocketContants {
	IOutputPipelineBuilder builder;
	OutputPipelineCollection pipelines;

	public OutputSelector(IServiceProvider parent) {
		this.builder = (IOutputPipelineBuilder) parent.getService("$.pipeline.output.builder");
	}
	@Override
	public IAsynchronizer select(Circuit circuit) throws CircuitException {
		if(!circuit.protocol().startsWith("HTTP")) {
			throw new CircuitException("503", "不支持的回路,协议："+circuit.protocol()+"。该方法仅支持http");
		}
		Asynchronizer asynchronizer = new Asynchronizer(circuit);
		return asynchronizer;
	}

	@Override
	public IOutputer select(Frame frame) throws CircuitException {
		String pipelineName = frame.head(__frame_fromPipelineName);
		if (StringUtil.isEmpty(pipelineName)) {
			new CircuitException("404", "不确定来源的侦");
		}
		String channelId=pipelineName.substring(0, pipelineName.indexOf("@"));
		String fromWho=frame.head(__frame_fromWho);
		String name=String.format("%s@%s", channelId,fromWho);
		return select(name);
	}

	@Override
	public IOutputer select(String name) throws CircuitException {
		if (pipelines == null) {
			this.pipelines = new OutputPipelineCollection();
		}
		IOutputPipeline output = pipelines.get(name);
		if (output != null) {
			if (output.isDisposed()) {
				pipelines.remove(name);
				output = null;
			} else {
				return output.handler();
			}
		}
		// 下面创建outputline
		output = builder.name(name).service("Output-Pipeline-Col", pipelines).createPipeline();
		// 激活管道
		try {
			output.headOnActive();
		} catch (Exception e) {
			CircuitException ce = CircuitException.search(e);
			if (ce != null) {
				ce.printStackTrace();
			}
			throw e;
		}
		pipelines.add(name, output);
		return output.handler();
	}

	class Asynchronizer implements IAsynchronizer {
		Circuit circuit;

		public Asynchronizer(Circuit circuit) {
			this.circuit = circuit;
		}

		@Override
		public void accept(IChunkVisitor visitor) {
			circuit.attribute(__circuit_chunk_visitor, visitor);
		}

	}
}
