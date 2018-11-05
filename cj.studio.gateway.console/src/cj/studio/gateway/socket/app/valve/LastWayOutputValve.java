package cj.studio.gateway.socket.app.valve;

import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.IJunctionTable;
import cj.studio.gateway.socket.pipeline.IOPipeline;
import cj.studio.gateway.socket.pipeline.IOutputValve;
import cj.studio.gateway.socket.pipeline.InputPipelineCollection;
import cj.ultimate.IClosable;
//在该类中对接目标socket中的input端子，如果请求的是cluster目标不存在，则启动它
//该类逻辑与net中实现的HttpServerHandler.java相同,并在本类中管道管理输出连接点
public class LastWayOutputValve implements IOutputValve,IClosable {
	private IJunctionTable invertJunctions;
	InputPipelineCollection pipelines;
	public LastWayOutputValve() {
//		invertJunctions = (IJunctionTable) parent.getService("$.junctions");
		this.pipelines = new InputPipelineCollection();
	}
	@Override
	public void flow(Object request, Object response, IOPipeline pipeline) throws CircuitException {
		
		
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

}
