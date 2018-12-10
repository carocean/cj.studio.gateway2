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
	//一定是单例模式，每次均申请一个管道，如果持有管道集合，则在多线程下，前面线程正要释放了管道还没从集合中移除，后面又获得了这个管道实例，则会导致拿着已释放的管道使用的bug
	@Override
	public IOutputer select(String name) throws CircuitException {
		// 下面创建outputline
		IOutputPipeline output = builder.name(name).createPipeline();
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
