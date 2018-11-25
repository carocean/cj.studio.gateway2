package cj.studio.gateway.socket.cable.wire;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
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
import cj.studio.gateway.socket.cable.IGatewaySocketWire;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import cj.studio.gateway.socket.pipeline.IInputPipelineBuilder;
import cj.studio.gateway.socket.pipeline.InputPipelineCollection;
import cj.studio.gateway.socket.serverchannel.ws.WebsocketServerChannelGatewaySocket;
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
import io.netty.util.CharsetUtil;

public class WSGatewaySocketWire implements IGatewaySocketWire {
	Channel channel;
	IServiceProvider parent;
	boolean isIdle;
	private long idleBeginTime;

	public WSGatewaySocketWire(IServiceProvider parent) {
		this.parent = parent;
		used(false);
	}

	@Override
	public void close() {
		ReentrantLock lock = (ReentrantLock) parent.getService("$.lock");
		try {
			lock.lock();
			@SuppressWarnings("unchecked")
			List<IGatewaySocketWire> wires = (List<IGatewaySocketWire>) parent.getService("$.wires");
			wires.remove(this);
			Condition waitingForCreateWire = (Condition) parent.getService("$.waitingForCreateWire");
			waitingForCreateWire.signalAll();// 通知新建
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void used(boolean b) {
		isIdle = !b;
		idleBeginTime = System.currentTimeMillis();
	}

	@Override
	public void dispose() {
		close();
		if (channel.isOpen()) {
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
	public Object send(Object request,Object response) throws CircuitException {
		Frame frame=(Frame)request;
		if (!channel.isWritable()) {// 断开连结，且从电缆中移除导线
			if (channel.isOpen()) {
				channel.close();
			}
			@SuppressWarnings("unchecked")
			List<IGatewaySocketWire> wires = (List<IGatewaySocketWire>) parent.getService("$.wires");
			wires.remove(this);
			return null;
		}
		WebSocketFrame f = new BinaryWebSocketFrame(frame.toByteBuf());
		channel.writeAndFlush(f);
		return null;
	}

	@Override
	public void connect(String ip, int port) throws CircuitException {
		EventLoopGroup group = (EventLoopGroup) parent.getService("$.eventloop.group");
		Bootstrap b = new Bootstrap();
		URI uri=null;
		String url=(String)parent.getService("$.prop.wspath");
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
		private ServerInfo info;

		public WebSocketClientHandler(WebSocketClientHandshaker handshaker) {
			this.handshaker = handshaker;
			sockets = (IGatewaySocketContainer) parent.getService("$.container.socket");
			junctions = (IJunctionTable) parent.getService("$.junctions");
			this.pipelines = new InputPipelineCollection();
			info = (ServerInfo) parent.getService("$.server.info");
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
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			String name = SocketName.name(ctx.channel().id(), info.getName());

			if (sockets.contains(name)) {
				sockets.remove(name);// 不论是ws还是http增加的，在此安全移除
			}

			pipelineRelease(name);
			@SuppressWarnings("unchecked")
			List<IGatewaySocketWire> wires = (List<IGatewaySocketWire>) parent.getService("$.wires");
			wires.remove(WSGatewaySocketWire.this);
		}

		@Override
		public void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
			Channel ch = ctx.channel();
			if (!handshaker.isHandshakeComplete()) {
				handshaker.finishHandshake(ch, (FullHttpResponse) msg);
//				System.out.println("WebSocket Client connected!");
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
			WebsocketServerChannelGatewaySocket wsSocket = new WebsocketServerChannelGatewaySocket(parent,
					ctx.channel());
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
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			cause.printStackTrace();

			if (!handshakeFuture.isDone()) {
				handshakeFuture.setFailure(cause);
			}

//			ctx.close();
		}
	}
}
