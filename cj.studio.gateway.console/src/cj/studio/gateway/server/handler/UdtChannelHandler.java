package cj.studio.gateway.server.handler;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.logging.ILogging;
import cj.studio.gateway.ICluster;
import cj.studio.gateway.IDestinationLoader;
import cj.studio.gateway.IGatewaySocketContainer;
import cj.studio.gateway.IJunctionTable;
import cj.studio.gateway.conf.ServerInfo;
import cj.studio.gateway.junction.ForwardJunction;
import cj.studio.gateway.junction.Junction;
import cj.studio.gateway.server.util.GetwayDestHelper;
import cj.studio.gateway.socket.Destination;
import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import cj.studio.gateway.socket.pipeline.IInputPipelineBuilder;
import cj.studio.gateway.socket.pipeline.InputPipelineCollection;
import cj.studio.gateway.socket.serverchannel.udt.UdtServerChannelGatewaySocket;
import cj.studio.gateway.socket.util.SocketContants;
import cj.studio.gateway.socket.util.SocketName;
import cj.ultimate.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.udt.UdtMessage;
import io.netty.handler.timeout.IdleStateEvent;

public class UdtChannelHandler extends ChannelHandlerAdapter implements ChannelHandler, SocketContants {
	IServiceProvider parent;
	public static ILogging logger;
	IGatewaySocketContainer sockets;
	private IJunctionTable junctions;
	InputPipelineCollection pipelines;
	private ServerInfo info;
	private int counter;

	public UdtChannelHandler(IServiceProvider parent) {
		this.parent = parent;
		logger = CJSystem.logging();
		sockets = (IGatewaySocketContainer) parent.getService("$.container.socket");
		junctions = (IJunctionTable) parent.getService("$.junctions");
		this.pipelines = new InputPipelineCollection();
		info = (ServerInfo) parent.getService("$.server.info");
	}
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			// 空闲6s之后触发 (心跳包丢失)
			if (counter >= 3) {
				// 连续丢失3个心跳包 (断开连接)
				ctx.channel().close().sync();
			} else {
				counter++;
			}
		} else {
			super.userEventTriggered(ctx, evt);
		}
	}
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object data) throws Exception {
		UdtMessage msg = (UdtMessage) data;
		ByteBuf bb = msg.content();
		byte[] b = new byte[bb.readableBytes()];
		bb.readBytes(b);
		Frame frame = new Frame(b);
		if ("NET/1.0".equals(frame.protocol())) {
			if ("heartbeat".equals(frame.command())) {
				return;
			}
		}
		String uri = frame.url();
		String gatewayDestInHeader = frame.head(__frame_gatewayDest);
		String gatewayDest = GetwayDestHelper.getGatewayDestForHttpRequest(uri, gatewayDestInHeader, getClass());
		if (StringUtil.isEmpty(gatewayDest) || gatewayDest.endsWith("://")) {
			throw new CircuitException("404", "缺少路由目标，请求侦被丢掉：" + uri);
		}

		String name = SocketName.name(ctx.channel().id(), info.getName());
		IInputPipeline inputPipeline = pipelines.get(name);
		// 检查目标管道是否存在
		if (inputPipeline != null) {
			Circuit circuit = new Circuit(String.format("%s 200 OK", frame.protocol()));
			inputPipeline.headFlow(frame, circuit);
			return;
		}

		// 以下生成目标管道
		pipelineBuild(name, gatewayDest, frame, ctx);
	}

	protected void pipelineBuild(String pipelineName, String gatewayDest, Frame frame, ChannelHandlerContext ctx)
			throws Exception {
		UdtServerChannelGatewaySocket wsSocket = new UdtServerChannelGatewaySocket(parent, ctx.channel());
		sockets.add(wsSocket);// 不放在channelActive方法内的原因是当有构建需要时才添加，是按需索求

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

		IInputPipelineBuilder builder = (IInputPipelineBuilder) socket.getService("$.pipeline.input.builder");
		IInputPipeline inputPipeline = builder.name(pipelineName).prop(__pipeline_fromProtocol, "udt")
				.prop(__pipeline_fromWho, info.getName()).createPipeline();
		pipelines.add(pipelineName, inputPipeline);

		ForwardJunction junction = new ForwardJunction(pipelineName);
		junction.parse(inputPipeline, ctx.channel(), socket);
		this.junctions.add(junction);

		Circuit circuit = new Circuit(String.format("%s 200 OK", frame.protocol()));
		inputPipeline.headOnActive(pipelineName);// 通知管道激活

		inputPipeline.headFlow(frame, circuit);// 再把本次请求发送处理
	}

	protected void pipelineRelease(String pipelineName) throws Exception {

		Junction junction = junctions.findInForwards(pipelineName);
		if (junction != null) {
			this.junctions.remove(junction);
		}

		IInputPipeline input = pipelines.get(pipelineName);
		if (input != null) {
			input.headOnInactive(pipelineName);
			pipelines.remove(pipelineName);
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		String name = SocketName.name(ctx.channel().id(), info.getName());

		if (sockets.contains(name)) {
			sockets.remove(name);// 在此安全移除
		}

		pipelineRelease(name);
		counter=0;
		super.channelInactive(ctx);
	}
}
