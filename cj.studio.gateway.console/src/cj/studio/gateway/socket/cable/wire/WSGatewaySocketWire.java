package cj.studio.gateway.socket.cable.wire;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.DefaultSegmentCircuit;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IContentReciever;
import cj.studio.ecm.net.IInputChannel;
import cj.studio.ecm.net.IOutputChannel;
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.ecm.net.io.MemoryInputChannel;
import cj.studio.gateway.IGatewaySocketContainer;
import cj.studio.gateway.IJunctionTable;
import cj.studio.gateway.junction.BackwardJunction;
import cj.studio.gateway.junction.Junction;
import cj.studio.gateway.server.util.GetwayDestHelper;
import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.cable.IGatewaySocketWire;
import cj.studio.gateway.socket.client.IExecutorPool;
import cj.studio.gateway.socket.io.WSOutputChannel;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import cj.studio.gateway.socket.pipeline.IInputPipelineBuilder;
import cj.studio.gateway.socket.pipeline.InputPipelineCollection;
import cj.studio.gateway.socket.util.SocketContants;
import cj.studio.gateway.socket.util.SocketName;
import cj.ultimate.util.StringUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;

public class WSGatewaySocketWire implements IGatewaySocketWire {
	Channel channel;
	IServiceProvider parent;
	volatile boolean isIdle;
	private long idleBeginTime;

	public WSGatewaySocketWire(IServiceProvider parent) {
		this.parent = parent;
	}

	@Override
	public void close() {
		@SuppressWarnings("unchecked")
		List<IGatewaySocketWire> wires = (List<IGatewaySocketWire>) parent.getService("$.wires");
		wires.remove(this);
	}

	@Override
	public void used(boolean b) {
		isIdle = !b;
		if (isIdle) {
			idleBeginTime = System.currentTimeMillis();
		}
	}

	@Override
	public void dispose() {
		close();
		if (channel != null && channel.isOpen()) {
			channel.close();
		}
	}

	@Override
	public long idleBeginTime() {
		return idleBeginTime;
	}

	@Override
	public boolean isIdle() {
		return isIdle;
	}

	@Override
	public synchronized Object send(Object request, Object response) throws CircuitException {
		if (!channel.isOpen()) {// 断开连结，且从电缆中移除导线
			@SuppressWarnings("unchecked")
			List<IGatewaySocketWire> wires = (List<IGatewaySocketWire>) parent.getService("$.wires");
			wires.remove(this);
			throw new CircuitException("500", "导线已关闭，包丢弃：" + request);
		}
		used(true);
		Frame frame = (Frame) request;
		if (!frame.content().isAllInMemory()) {
			throw new CircuitException("503", "ws协议仅支持内存模式，请使用MemoryContentReciever");
		}
		WebSocketFrame f = new BinaryWebSocketFrame(frame.toByteBuf());
		ChannelFuture future =channel.writeAndFlush(f);
		try {
			future.await(SocketContants.__channel_write_await_timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		used(false);
		return null;
	}

	@Override
	public void connect(String ip, int port) throws CircuitException {
		IExecutorPool exepool = (IExecutorPool) parent.getService("$.executor.pool");
		EventLoopGroup group = exepool.getEventLoopGroup();
		Bootstrap b = new Bootstrap();
		URI uri = null;
		String url = (String) parent.getService("$.prop.wspath");
		try {
			uri = new URI(url);
		} catch (URISyntaxException e1) {
			throw new CircuitException("503", e1);
		}
		HttpHeaders customHeaders = new DefaultHttpHeaders();
//		customHeaders.add("MyHeader", "MyValue");
		WebSocketClientHandler handler = new WebSocketClientHandler(
				WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, false, customHeaders));
		b.group(group).channel(NioSocketChannel.class).handler(new WsClientGatewaySocketInitializer(handler));
		try {
			this.channel = b.connect(ip, port).sync().channel();
			handler.handshakeFuture().sync();
		} catch (Throwable e) {
			used(false);
			@SuppressWarnings("unchecked")
			List<IGatewaySocketWire> wires = (List<IGatewaySocketWire>) parent.getService("$.wires");
			wires.remove(this);
			throw new CircuitException("505", e);
		}
		used(false);
	}

	@Override
	public boolean isWritable() {
		return channel.isWritable();
	}

	@Override
	public boolean isOpened() {
		return channel.isOpen();
	}

	class WsClientGatewaySocketInitializer extends ChannelInitializer<SocketChannel> {
		WebSocketClientHandler handler;

		public WsClientGatewaySocketInitializer(WebSocketClientHandler handler) {
			this.handler = handler;
		}

		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			ChannelPipeline pipeline = ch.pipeline();
			int aggregatorLimit = (int) parent.getService("$.prop.aggregatorLimit");
			pipeline.addLast("http-codec", new HttpClientCodec());
			long interval = (long) parent.getService("$.prop.heartbeat");
			if (interval > 0) {
				pipeline.addLast(new IdleStateHandler(0, 0, interval, TimeUnit.MILLISECONDS));
			}
			pipeline.addLast("aggregator", new HttpObjectAggregator(aggregatorLimit));
			pipeline.addLast("ws-handler", handler);

		}

	}

	class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> implements SocketContants {
		// 直接搜到目标的input传入即可,因此clientsocket不需要输出管道，性能更好。
		// 这与server端向目标端的输出一致。
		private final WebSocketClientHandshaker handshaker;
		private ChannelPromise handshakeFuture;
		IGatewaySocketContainer sockets;
		private IJunctionTable junctions;
		InputPipelineCollection pipelines;
		private String socketName;

		public WebSocketClientHandler(WebSocketClientHandshaker handshaker) {
			this.handshaker = handshaker;
			sockets = (IGatewaySocketContainer) parent.getService("$.container.socket");
			junctions = (IJunctionTable) parent.getService("$.junctions");
			this.pipelines = new InputPipelineCollection();
			socketName = (String) parent.getService("$.socket.name");
		}

		public ChannelFuture handshakeFuture() {
			return handshakeFuture;
		}

		@Override
		public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
			handshakeFuture = ctx.newPromise();
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			handshaker.handshake(ctx.channel());
			String gatewayDests = (String)parent.getService("$.prop."+__channel_onchannelEvent_notify_dests);
			if (StringUtil.isEmpty(gatewayDests)) {
				CJSystem.logging().warn(getClass(), String.format(
						"客户端：%s 未指定通道激活或失活事件的通知目标。应用仅能在之后第一次请求时才能收到激活或失活事件。请在该net的连接串中指定参数：OnChannelEvent-Notify-Dests=dest1,dest2",
						parent.getService("$.socket.name")));
				return;
			}
			String arr[] = gatewayDests.split(",");
			for (String gatewayDest : arr) {
				Frame frame = new Frame(String.format("onactive /%s/ ws/1.0", gatewayDest));
				WSOutputChannel output = new WSOutputChannel(ctx.channel(), frame);
				Circuit circuit = new DefaultSegmentCircuit(output, String.format("%s 200 OK", frame.protocol()));
				pipelineBuild(gatewayDest, circuit, ctx);
			}
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			pipelineRelease(ctx);
		}

		protected void pipelineRelease(ChannelHandlerContext ctx) throws Exception {
			Set<String> dests = pipelines.enumDest();
			for (String dest : dests) {
				String pipelineName = SocketName.name(ctx.channel().id(), dest);
				Junction junction = junctions.findInBackwards(pipelineName);
				if (junction != null) {
					this.junctions.remove(junction);
				}
				IInputPipeline input = pipelines.get(dest);
				input.headOnInactive(pipelineName);
			}
			pipelines.dispose();

			WSGatewaySocketWire.this.close();
		}

		@Override
		public void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
			Channel ch = ctx.channel();
			if (!handshaker.isHandshakeComplete()) {
				handshaker.finishHandshake(ch, (FullHttpResponse) msg);
				handshakeFuture.setSuccess();
				return;
			}
			if (msg instanceof PongWebSocketFrame) {
				return;
			}
			if (msg instanceof CloseWebSocketFrame) {
				ch.close();
				return;
			}
			if (msg instanceof FullHttpResponse) {
				FullHttpResponse response = (FullHttpResponse) msg;
				throw new Exception("Unexpected FullHttpResponse (getStatus=" + response.getStatus() + ", content="
						+ response.content().toString(CharsetUtil.UTF_8) + ')');
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
			IContentReciever reciever = new MemoryContentReciever();
			Frame frame = new Frame(input,reciever, b);
			input.begin(frame);
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
			inputPipeline=pipelineBuild(gatewayDest, circuit, ctx);
			flowPipeline(ctx, inputPipeline, frame, circuit);// 再把本次请求发送处理
		}

		protected void flowPipeline(ChannelHandlerContext ctx, IInputPipeline pipeline, Frame frame, Circuit circuit)
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

		protected IInputPipeline pipelineBuild(String gatewayDest,  Circuit circuit, ChannelHandlerContext ctx)
				throws Exception {
			IGatewaySocket socket = this.sockets.getAndCreate(gatewayDest);

			IInputPipelineBuilder builder = (IInputPipelineBuilder) socket.getService("$.pipeline.input.builder");
			String pipelineName = SocketName.name(ctx.channel().id(), gatewayDest);
			IInputPipeline inputPipeline = builder.name(pipelineName).prop(__pipeline_fromProtocol, "ws")
					.prop(__pipeline_fromWho, socketName).createPipeline();
			pipelines.add(gatewayDest, inputPipeline);

			BackwardJunction junction = new BackwardJunction(pipelineName);
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

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			if (!handshakeFuture.isDone()) {
				handshakeFuture.setFailure(cause);
			}

			super.exceptionCaught(ctx, cause);
		}

		@Override
		public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
			if (evt instanceof IdleStateEvent) {
				sendHeartbeatPacket(ctx);
			} else {
				super.userEventTriggered(ctx, evt);
			}
		}

		private void sendHeartbeatPacket(ChannelHandlerContext ctx) {
			PongWebSocketFrame pong = new PongWebSocketFrame();
			ctx.writeAndFlush(pong);

		}
	}
}
