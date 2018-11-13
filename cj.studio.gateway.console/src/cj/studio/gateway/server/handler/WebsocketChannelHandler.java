package cj.studio.gateway.server.handler;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
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
import cj.studio.gateway.socket.serverchannel.ws.WebsocketServerChannelGatewaySocket;
import cj.studio.gateway.socket.util.SocketContants;
import cj.studio.gateway.socket.util.SocketName;
import cj.ultimate.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public class WebsocketChannelHandler extends SimpleChannelInboundHandler<Object>
		implements ChannelHandler, SocketContants {
	IServiceProvider parent;
	public static ILogging logger;
	IGatewaySocketContainer sockets;
	private IJunctionTable junctions;
	InputPipelineCollection pipelines;
	private ServerInfo info;

	public WebsocketChannelHandler(IServiceProvider parent) {
		this.parent = parent;
		logger = CJSystem.logging();
		sockets = (IGatewaySocketContainer) parent.getService("$.container.socket");
		junctions = (IJunctionTable) parent.getService("$.junctions");
		this.pipelines = new InputPipelineCollection();
		info = (ServerInfo) parent.getService("$.server.info");
	}
	
	@Override
	protected void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf bb =null;
		if(msg instanceof TextWebSocketFrame) {
			TextWebSocketFrame f=(TextWebSocketFrame)msg;
			bb=f.content();
		}else if(msg instanceof BinaryWebSocketFrame){
			BinaryWebSocketFrame f=(BinaryWebSocketFrame)msg;
			bb=f.content();
		}else {
			throw new EcmException("不支持此类消息："+msg.getClass());
		}
		
		byte[] b = new byte[bb.readableBytes()];
		bb.readBytes(b);
		Frame frame = new Frame(b);

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
		WebsocketServerChannelGatewaySocket wsSocket = new WebsocketServerChannelGatewaySocket(parent, ctx.channel());
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
		IInputPipeline inputPipeline = builder.name(pipelineName).prop(__pipeline_fromProtocol, "ws")
				.prop(__pipeline_fromWho, info.getName()).createPipeline();
		pipelines.add(pipelineName, inputPipeline);

		ForwardJunction junction = new ForwardJunction(pipelineName);
		junction.parse(inputPipeline, ctx.channel(), socket);
		this.junctions.add(junction);

		Circuit circuit = new Circuit(String.format("%s 200 OK", frame.protocol()));
		inputPipeline.headOnActive(pipelineName);// 通知管道激活

		inputPipeline.headFlow(frame, circuit);//再把本次请求发送处理
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
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		
		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		String name = SocketName.name(ctx.channel().id(), info.getName());

		if (sockets.contains(name)) {
			sockets.remove(name);// 不论是ws还是http增加的，在此安全移除
		}

		pipelineRelease(name);

		super.channelInactive(ctx);
	}
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		// TODO Auto-generated method stub
		super.exceptionCaught(ctx, cause);
	}
}
