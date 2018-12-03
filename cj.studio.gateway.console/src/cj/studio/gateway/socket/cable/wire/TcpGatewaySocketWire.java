package cj.studio.gateway.socket.cable.wire;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.net.nio.netty.tcp.TcpFrameBox;
import cj.studio.gateway.IGatewaySocketContainer;
import cj.studio.gateway.IJunctionTable;
import cj.studio.gateway.conf.ServerInfo;
import cj.studio.gateway.junction.ForwardJunction;
import cj.studio.gateway.junction.Junction;
import cj.studio.gateway.server.util.GetwayDestHelper;
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
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

public class TcpGatewaySocketWire implements IGatewaySocketWire {
	Channel channel;
	IServiceProvider parent;
	volatile boolean isIdle;
	private long idleBeginTime;

	public TcpGatewaySocketWire(IServiceProvider parent) {
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
		if (isIdle) {
			ReentrantLock lock = (ReentrantLock) parent.getService("$.lock");
			try {
				lock.lock();
				Condition waitingForCreateWire = (Condition) parent.getService("$.waitingForCreateWire");
				waitingForCreateWire.signalAll();// 通知新建

			} finally {
				lock.unlock();
			}
		}
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
	public synchronized Object send(Object request, Object response) throws CircuitException {
		Frame frame = (Frame) request;
		if (!channel.isWritable()) {// 断开连结，且从电缆中移除导线
			if (channel.isOpen()) {
				channel.close();
			}
			@SuppressWarnings("unchecked")
			List<IGatewaySocketWire> wires = (List<IGatewaySocketWire>) parent.getService("$.wires");
			wires.remove(this);
			return null;
		}
		byte[] box = TcpFrameBox.box(frame.toBytes());
		ByteBuf bb = Unpooled.directBuffer();
		bb.writeBytes(box);
		channel.writeAndFlush(bb);
		return null;
	}

	@Override
	public void connect(String ip, int port) throws CircuitException {
		EventLoopGroup group = (EventLoopGroup) parent.getService("$.eventloop.group");
		Bootstrap b = new Bootstrap();

		b.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
				.handler(new TcpClientGatewaySocketInitializer());
		try {
			this.channel = b.connect(ip, port).sync().channel();
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

	class TcpClientGatewaySocketInitializer extends ChannelInitializer<SocketChannel> {

		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			ChannelPipeline pipeline = ch.pipeline();
			// 心跳就不必加了，电缆是连接池，如果发现某个导线断开，会移除掉重新选择一根，所以永远是连接的。
//			int heartbeat = (int) parent.getService("$.prop.heartbeat");
//			if (heartbeat > 0) {
//				pipeline.addLast(new IdleStateHandler(0, 5, 0));// 写心跳
//			}
			/*
			 * 这个地方的 必须和服务端对应上。否则无法正常解码和编码
			 * 
			 * 解码和编码 我将会在下一张为大家详细的讲解。再次暂时不做详细的描述
			 */
			pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
			int interval=(int)parent.getService(SocketContants.__key_heartbeat_interval);
			if(interval>0) {
				pipeline.addLast(new IdleStateHandler(0, 0, interval, TimeUnit.SECONDS));
			}
			pipeline.addLast(new TcpClientHandler());

		}

	}

	class TcpClientHandler extends SimpleChannelInboundHandler<Object> implements SocketContants {
		// 直接搜到目标的input传入即可,因此clientsocket不需要输出管道，性能更好。
		// 这与server端向目标端的输出一致。
		IGatewaySocketContainer sockets;
		private IJunctionTable junctions;
		InputPipelineCollection pipelines;
		private ServerInfo info;

		public TcpClientHandler() {
			sockets = (IGatewaySocketContainer) parent.getService("$.container.socket");
			junctions = (IJunctionTable) parent.getService("$.junctions");
			this.pipelines = new InputPipelineCollection();
			info = (ServerInfo) parent.getService("$.server.info");
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
			wires.remove(TcpGatewaySocketWire.this);
		}

		@Override
		public void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
			ByteBuf bb = (ByteBuf) msg;
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

			IGatewaySocket socket = this.sockets.getAndCreate(gatewayDest);

			IInputPipelineBuilder builder = (IInputPipelineBuilder) socket.getService("$.pipeline.input.builder");
			IInputPipeline inputPipeline = builder.name(pipelineName).prop(__pipeline_fromProtocol, "tcp")
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
		public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
			if (evt instanceof IdleStateEvent) {
				// 不管是读事件空闲还是写事件空闲都向服务器发送心跳包
				sendHeartbeatPacket(ctx);
			} else {
				super.userEventTriggered(ctx, evt);
			}
		}

		private void sendHeartbeatPacket(ChannelHandlerContext ctx) {
			Frame f = new Frame("heartbeat / net/1.0");
			byte[] box = TcpFrameBox.box(f.toBytes());
			ByteBuf bb = Unpooled.directBuffer();
			bb.writeBytes(box);
			ctx.writeAndFlush(bb);

		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			cause.printStackTrace();
//			ctx.close();
		}
	}
}
