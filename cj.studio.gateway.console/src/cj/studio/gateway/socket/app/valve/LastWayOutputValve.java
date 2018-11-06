package cj.studio.gateway.socket.app.valve;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.ICluster;
import cj.studio.gateway.IDestinationLoader;
import cj.studio.gateway.IGatewaySocketContainer;
import cj.studio.gateway.IJunctionTable;
import cj.studio.gateway.junction.InvertJunction;
import cj.studio.gateway.junction.Junction;
import cj.studio.gateway.socket.Destination;
import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import cj.studio.gateway.socket.pipeline.IInputPipelineBuilder;
import cj.studio.gateway.socket.pipeline.IOPipeline;
import cj.studio.gateway.socket.pipeline.IOutputValve;
import cj.studio.gateway.socket.ws.WebsocketGatewaySocket;
import cj.ultimate.IClosable;

//在该类中对接目标socket中的input端子，如果请求的是cluster目标不存在，则启动它
//该类逻辑与net中实现的HttpServerHandler.java相同,并在本类中管道管理输出连接点
public class LastWayOutputValve implements IOutputValve, IClosable {
	private IJunctionTable invertJunctions;
	private IServiceProvider parent;
	private IGatewaySocketContainer sockets;
	private IGatewaySocket targetSocket;
	private IInputPipeline input;// 一个输出端子对应一个目标的输入端子
	boolean isDispose;
	public LastWayOutputValve(IServiceProvider parent) {
		this.parent = parent;
		sockets = (IGatewaySocketContainer) parent.getService("$.container.socket");
		invertJunctions = (IJunctionTable) parent.getService("$.junctions");
	}

	@Override
	public void flow(Object request, Object response, IOPipeline pipeline) throws CircuitException {
		if (!(request instanceof Frame)) {
			return;
		}

		input.headFlow(request, response);
	}

	@Override
	public void close() {
		if (targetSocket instanceof WebsocketGatewaySocket) {
			try {
				targetSocket.close();
			} catch (CircuitException e) {
				throw new EcmException(e);
			}
		}

		dispose(null);
	}

	@Override
	public void onActive(IOPipeline pipeline) throws CircuitException {
		String gatewayDest = pipeline.prop("To-Name");
		IGatewaySocket socket = this.sockets.find(gatewayDest);
		if (socket == null) {
			ICluster cluster = (ICluster) parent.getService("$.cluster");
			Destination destination = cluster.getDestination(gatewayDest);
			if (destination == null) {
				throw new CircuitException("404", "簇中缺少目标:" + gatewayDest);
			}
			IDestinationLoader loader = (IDestinationLoader) parent.getService("$.dloader");
			socket = loader.load(destination);
			sockets.add(socket);
		}
		this.targetSocket = socket;

		// 以下求一个输入端子
		IInputPipelineBuilder builder = (IInputPipelineBuilder) targetSocket.getService("$.pipeline.input.builder");
		String name = gatewayDest;
		IInputPipeline inputPipeline = builder.name(name).prop("From-Protocol", pipeline.prop("From-Protocol"))
				.prop("From-Name", pipeline.prop("From-Name")).prop("To-Name", targetSocket.name()).createPipeline();
		input = inputPipeline;

		InvertJunction junction = new InvertJunction(name);
		junction.parse(inputPipeline, targetSocket);
		this.invertJunctions.add(junction);

	}

	@Override
	public void onInactive(IOPipeline pipeline) throws CircuitException {
		dispose(pipeline);
	}

	protected void dispose(IOPipeline pipeline) {
		if(isDispose) {
			return;
		}
		if (pipeline != null) {
			String gatewayDest = pipeline.prop("To-Name");
			String name = gatewayDest;
			Junction junction = invertJunctions.findInForwards(name);
			if (junction != null) {
				this.invertJunctions.remove(junction);
			}
		}
		input.dispose();
		input = null;
		this.invertJunctions = null;
		this.sockets = null;
		this.parent = null;
		this.targetSocket = null;
		isDispose=true;
	}
}
