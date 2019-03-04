package cj.studio.gateway.server.handler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.logging.ILogging;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IInputChannel;
import cj.studio.ecm.net.IOutputChannel;
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.ecm.net.io.MemoryInputChannel;
import cj.studio.gateway.IGatewaySocketContainer;
import cj.studio.gateway.IJunctionTable;
import cj.studio.gateway.conf.ServerInfo;
import cj.studio.gateway.junction.ForwardJunction;
import cj.studio.gateway.junction.Junction;
import cj.studio.gateway.server.util.GetwayDestHelper;
import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.io.WSOutputChannel;
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
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;

public class WebsocketChannelHandler extends SimpleChannelInboundHandler<Object>
		implements ChannelHandler, SocketContants {
	IServiceProvider parent;
	public static ILogging logger;
	IGatewaySocketContainer sockets;
	private IJunctionTable junctions;
	InputPipelineCollection pipelines;
	private ServerInfo info;
	// 心跳丢失计数器
	private int counter;

	public WebsocketChannelHandler(IServiceProvider parent) {
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
	protected void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof PongWebSocketFrame) {
			ctx.channel().write(new PongWebSocketFrame(((WebSocketFrame) msg).content().retain()));
			return;
		}
		if (msg instanceof PingWebSocketFrame) {
			ctx.channel().write(new PingWebSocketFrame(((WebSocketFrame) msg).content().retain()));
			return;
		}
		if (msg instanceof CloseWebSocketFrame) {
			ctx.close();
			return;
		}
		ByteBuf bb = null;
		if (msg instanceof TextWebSocketFrame) {
			TextWebSocketFrame f = (TextWebSocketFrame) msg;
			bb = f.content();
		} else if (msg instanceof BinaryWebSocketFrame) {
			BinaryWebSocketFrame f = (BinaryWebSocketFrame) msg;
			bb = f.content();
		} else {
			throw new EcmException("不支持此类消息：" + msg.getClass());
		}
		if (bb.readableBytes() == 0) {
			return;
		}
		byte[] b = new byte[bb.readableBytes()];
		bb.readBytes(b);

		IInputChannel input = new MemoryInputChannel(8192);
		MemoryContentReciever rec = new MemoryContentReciever();
		Frame frame = new Frame(input, rec, b);
		input.begin(null);
		input.done(b, 0, 0);

		String uri = frame.url();
		String gatewayDestInHeader = frame.head(__frame_gatewayDest);
		String gatewayDest = GetwayDestHelper.getGatewayDestForHttpRequest(uri, gatewayDestInHeader, getClass());
		if (StringUtil.isEmpty(gatewayDest) || gatewayDest.endsWith("://")) {
			throw new CircuitException("404", "缺少路由目标，请求侦被丢掉：" + uri);
		}
		IOutputChannel output = new WSOutputChannel(ctx.channel(), frame);
		Circuit circuit = new Circuit(output, String.format("%s 200 OK", frame.protocol()));
		IInputPipeline inputPipeline = pipelines.get(gatewayDest);
		// 检查目标管道是否存在
		if (inputPipeline != null) {
			flowPipeline(ctx, inputPipeline, frame, circuit);
			return;
		}

		// 以下生成目标管道
		inputPipeline = pipelineBuild(gatewayDest, circuit, ctx);
		flowPipeline(ctx, inputPipeline, frame, circuit);// 再把本次请求发送处理
	}

	private void flowPipeline(ChannelHandlerContext ctx, IInputPipeline pipeline, Frame frame, Circuit circuit)
			throws Exception {
		frame.head(__frame_fromProtocol, pipeline.prop(__pipeline_fromProtocol));
		frame.head(__frame_fromWho, pipeline.prop(__pipeline_fromWho));
		frame.head(__frame_fromPipelineName, pipeline.prop(__pipeline_name));

		try {
			pipeline.headFlow(frame, circuit);
		} catch (Throwable e) {
			if (!circuit.content().isCommited()) {
				circuit.content().clearbuf();
				circuit.content().flush();
			}
			StringWriter out = new StringWriter();
			e.printStackTrace(new PrintWriter(out));
			circuit.content().writeBytes(out.toString().getBytes());
			circuit.content().flush();
			throw e;
		} finally {
			if (!circuit.content().isClosed()) {
				circuit.content().close();
			}
		}
	}

	protected IInputPipeline pipelineBuild(String gatewayDest, Circuit circuit, ChannelHandlerContext ctx)
			throws Exception {
		WebsocketServerChannelGatewaySocket wsSocket = new WebsocketServerChannelGatewaySocket(parent,gatewayDest, ctx.channel());
		sockets.add(wsSocket);// 不放在channelActive方法内的原因是当有构建需要时才添加，是按需索求

		IGatewaySocket socket = this.sockets.getAndCreate(gatewayDest);

		String pipelineName = SocketName.name(ctx.channel().id(), gatewayDest);
		IInputPipelineBuilder builder = (IInputPipelineBuilder) socket.getService("$.pipeline.input.builder");
		IInputPipeline inputPipeline = builder.name(pipelineName).prop(__pipeline_fromProtocol, "ws")
				.prop(__pipeline_fromWho, info.getName()).createPipeline();
		pipelines.add(gatewayDest, inputPipeline);

		ForwardJunction junction = new ForwardJunction(pipelineName);
		junction.parse(inputPipeline, ctx.channel(), socket);
		this.junctions.add(junction);

		try {
			inputPipeline.headOnActive(pipelineName);// 通知管道激活
		} catch (Exception e) {
			if (!circuit.content().isCommited()) {
				circuit.content().clearbuf();
				circuit.content().flush();
			}
			StringWriter out = new StringWriter();
			e.printStackTrace(new PrintWriter(out));
			circuit.content().writeBytes(out.toString().getBytes());
			circuit.content().close();
			ctx.close();
			throw e;
		}
		return inputPipeline;
	}

	protected void pipelineRelease(ChannelHandlerContext ctx) throws Exception {
		Set<String> dests = pipelines.enumDest();
		for (String dest : dests) {
			String pipelineName = SocketName.name(ctx.channel().id(), dest);
			Junction junction = junctions.findInForwards(pipelineName);
			if (junction != null) {
				this.junctions.remove(junction);
			}
			IInputPipeline input = pipelines.get(dest);
			input.headOnInactive(pipelineName);
		}
		pipelines.dispose();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		String gatewayDests = this.info.getProps().get(__channel_onchannelEvent_notify_dests);
		if (StringUtil.isEmpty(gatewayDests)) {
			CJSystem.logging().warn(getClass(), String.format(
					"服务器：%s 未指定通道激活或失活事件的通知目标。应用仅能在之后第一次请求时才能收到激活或失活事件。请在该net的属性中指定：OnChannelEvent-Notify-Dests=dest1,dest2",
					info.getName()));
			return;
		}
		String arr[] = gatewayDests.split(",");
		for (String gatewayDest : arr) {
			if(StringUtil.isEmpty(gatewayDest))continue;
			Frame frame = new Frame(String.format("onactive /%s/ ws/1.0", gatewayDest));
			WSOutputChannel output = new WSOutputChannel(ctx.channel(), frame);
			Circuit circuit = new Circuit(output, String.format("%s 200 OK", frame.protocol()));
			pipelineBuild(gatewayDest, circuit, ctx);
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		String name = SocketName.name(ctx.channel().id(), info.getName());

		if (sockets.contains(name)) {
			sockets.remove(name);// 不论是ws还是http增加的，在此安全移除
		}

		pipelineRelease(ctx);

		super.channelInactive(ctx);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		// TODO Auto-generated method stub
		super.exceptionCaught(ctx, cause);
	}
}
