package cj.studio.gateway.socket.app.valve;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.IGatewaySocketContainer;
import cj.studio.gateway.IJunctionTable;
import cj.studio.gateway.junction.BackwardJunction;
import cj.studio.gateway.junction.Junction;
import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import cj.studio.gateway.socket.pipeline.IInputPipelineBuilder;
import cj.studio.gateway.socket.pipeline.IOPipeline;
import cj.studio.gateway.socket.pipeline.IOutputValve;
import cj.studio.gateway.socket.pipeline.IValveDisposable;
import cj.studio.gateway.socket.util.SocketContants;

//在该类中对接目标socket中的input端子，如果请求的是cluster目标不存在，则启动它
//该类逻辑与net中实现的HttpServerHandler.java相同,并在本类中管道管理输出连接点
public class LastWayOutputValve implements IOutputValve, IValveDisposable, SocketContants {
	private IJunctionTable junctions;
	private IGatewaySocketContainer sockets;
	private IGatewaySocket targetSocket;
	private IInputPipeline input;// 一个输出端子对应一个目标的输入端子
	boolean isDisposed;

	public LastWayOutputValve(IServiceProvider parent) {
		sockets = (IGatewaySocketContainer) parent.getService("$.container.socket");
		junctions = (IJunctionTable) parent.getService("$.junctions");
	}

	@Override
	public void flow(Object request, Object response, IOPipeline pipeline) throws CircuitException {
		if (!(request instanceof Frame)) {
			return;
		}

		input.headFlow(request, response);
	}

	@Override
	public void onActive(IOPipeline pipeline) throws CircuitException {
		String pipelineName = pipeline.prop(SocketContants.__pipeline_name);// 在backward输出管道中，由于一个输出管道仅对应一个输入管道，因此管道名即为目标
		IGatewaySocket socket = this.sockets.getAndCreate(pipelineName);
		this.targetSocket = socket;

		// 以下求一个输入端子
		IInputPipelineBuilder builder = (IInputPipelineBuilder) targetSocket.getService("$.pipeline.input.builder");
		String name = pipelineName;
		IInputPipeline inputPipeline = builder.name(name)
				.prop(__pipeline_fromProtocol, pipeline.prop(__pipeline_fromProtocol))
				.prop(__pipeline_fromWho, pipeline.prop(__pipeline_fromWho)).createPipeline();
		input = inputPipeline;

		BackwardJunction junction = new BackwardJunction(name);
		junction.parse(pipeline, inputPipeline, targetSocket);
		this.junctions.add(junction);

		input.headOnActive(name);
	}

	@Override
	public void onInactive(IOPipeline pipeline) throws CircuitException {
		String pipelineName = pipeline.prop(SocketContants.__pipeline_name);
		Junction junction = junctions.findInBackwards(pipelineName);
		if (junction != null) {
			this.junctions.remove(junction);
		}
		try {
			input.headOnInactive(pipelineName);
		} catch (CircuitException e) {
			throw new EcmException(e);
		}
	}
	
	@Override
	public void dispose(boolean isCloseableOutputValve) {
		if (isDisposed) {
			return;
		}
		input.dispose(isCloseableOutputValve);
		input = null;
		this.junctions = null;
		this.sockets = null;
		this.targetSocket = null;
		isDisposed = true;
	}
}
