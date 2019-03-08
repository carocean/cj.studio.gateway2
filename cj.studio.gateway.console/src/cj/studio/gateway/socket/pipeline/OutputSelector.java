package cj.studio.gateway.socket.pipeline;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.socket.util.SocketContants;
import cj.ultimate.util.StringUtil;

public class OutputSelector implements IOutputSelector, SocketContants {
	IOutputPipelineBuilder builder;

	public OutputSelector(IServiceProvider parent) {
		this.builder = (IOutputPipelineBuilder) parent.getService("$.pipeline.output.builder");
	}

	@Override
	public IOutputer select(Frame frame) throws CircuitException {
		String pipelineName = frame.head(__frame_fromPipelineName);
		if (StringUtil.isEmpty(pipelineName)) {
			new CircuitException("404", "不确定来源的侦");
		}
		return select(pipelineName);
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
				CJSystem.logging().error(getClass(), ce);
			}
			throw e;
		}
		return output.handler();
	}

}
