package cj.studio.gateway.socket.cable.wire;

import java.io.PrintWriter;
import java.io.StringWriter;
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
import cj.studio.ecm.net.IInputChannel;
import cj.studio.ecm.net.IOutputChannel;
import cj.studio.ecm.net.http.HttpCircuit;
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.ecm.net.io.MemoryInputChannel;
import cj.studio.ecm.net.io.SimpleInputChannel;
import cj.studio.ecm.net.util.TcpFrameBox;
import cj.studio.gateway.IGatewaySocketContainer;
import cj.studio.gateway.IJunctionTable;
import cj.studio.gateway.junction.BackwardJunction;
import cj.studio.gateway.junction.Junction;
import cj.studio.gateway.server.util.GetwayDestHelper;
import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.cable.IGatewaySocketWire;
import cj.studio.gateway.socket.cable.wire.reciever.TcpContentReciever;
import cj.studio.gateway.socket.client.IExecutorPool;
import cj.studio.gateway.socket.io.TcpInputChannel;
import cj.studio.gateway.socket.io.TcpOutputChannel;
import cj.studio.gateway.socket.io.WSOutputChannel;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import cj.studio.gateway.socket.pipeline.IInputPipelineBuilder;
import cj.studio.gateway.socket.pipeline.InputPipelineCollection;
import cj.studio.gateway.socket.util.SocketContants;
import cj.studio.gateway.socket.util.SocketName;
import cj.ultimate.util.StringUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
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
		if (channel != null) {
			if (channel.isOpen()) {
				channel.close();
			}
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
		byte[] b = null;
		if (frame.content().hasReciever()) {
			if (!frame.content().isAllInMemory()) {
				throw new CircuitException("503", "TCP仅支持MemoryContentReciever或者内容接收器为空." + frame);
			}
			if (frame.content().revcievedBytes() > 0) {
				b = frame.content().readFully();
			}
		}
		TcpContentReciever tcr = new TcpContentReciever(channel);
		frame.content().accept(tcr);// 不管是否已存在接收器都覆盖掉

		MemoryInputChannel in = new MemoryInputChannel(8192);
		Frame pack = new Frame(in, "frame / gateway/1.0");// 有三种包：frame,content,last。frame包无内容；content和last包有内容无头
		pack.content().accept(new MemoryContentReciever());
		in.begin(null);
		byte[] data = frame.toBytes();
		in.done(data, 0, data.length);

		byte[] box = TcpFrameBox.box(pack.toBytes());
		ByteBuf bb = Unpooled.buffer();
		bb.writeBytes(box);
		ChannelFuture future = channel.writeAndFlush(bb);
		try {
			future.await(SocketContants.__channel_write_await_timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (b != null) {
			tcr.done(b, 0, b.length);
		}
		used(false);
		return null;
	}

	@Override
	public void connect(String ip, int port) throws CircuitException {
		IExecutorPool exepool = (IExecutorPool) parent.getService("$.executor.pool");
		EventLoopGroup group = exepool.getEventLoopGroup();
		Bootstrap b = new Bootstrap();

		b.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
				.handler(new TcpClientGatewaySocketInitializer());
		try {
			this.channel = b.connect(ip, port).sync().channel();
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

	class TcpClientGatewaySocketInitializer extends ChannelInitializer<SocketChannel> {

		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			ChannelPipeline pipeline = ch.pipeline();
			/*
			 * 这个地方的 必须和服务端对应上。否则无法正常解码和编码
			 * 
			 * 解码和编码 我将会在下一张为大家详细的讲解。再次暂时不做详细的描述
			 */
			pipeline.addLast(new LengthFieldBasedFrameDecoder(81920, 0, 4, 0, 4));
			long interval = (long) parent.getService("$.prop.heartbeat");
			if (interval > 0) {
				pipeline.addLast(new IdleStateHandler(0, 0, interval, TimeUnit.MILLISECONDS));
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
		private String socketName;
		private Circuit currentCircuit;
		private TcpInputChannel inputChannel;

		public TcpClientHandler() {
			sockets = (IGatewaySocketContainer) parent.getService("$.container.socket");
			junctions = (IJunctionTable) parent.getService("$.junctions");
			this.pipelines = new InputPipelineCollection();
			socketName = (String) parent.getService("$.socket.name");
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			pipelineRelease(ctx);

		}
		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			super.channelActive(ctx);
			String gatewayDests = (String)parent.getService("$.prop."+__channel_onchannelEvent_notify_dests);
			if (StringUtil.isEmpty(gatewayDests)) {
				CJSystem.logging().warn(getClass(), String.format(
						"客户端：%s 未指定通道激活或失活事件的通知目标。应用仅能在之后第一次请求时才能收到激活或失活事件。请在该net的连接串中指定参数：OnChannelEvent-Notify-Dests=dest1,dest2",
						parent.getService("$.socket.name")));
				return;
			}
			String arr[] = gatewayDests.split(",");
			for (String gatewayDest : arr) {
				Frame frame = new Frame(String.format("onactive /%s/ tcp/1.0", gatewayDest));
				WSOutputChannel output = new WSOutputChannel(ctx.channel(), frame);
				Circuit circuit = new DefaultSegmentCircuit(output, String.format("%s 200 OK", frame.protocol()));
				pipelineBuild(gatewayDest, circuit, ctx);
			}
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

			TcpGatewaySocketWire.this.close();
		}

		@Override
		public void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {

			ByteBuf bb = (ByteBuf) msg;
			if (bb.readableBytes() == 0) {
				return;
			}
			byte[] b = new byte[bb.readableBytes()];
			bb.readBytes(b);
			IInputChannel input = new MemoryInputChannel(8192);
			MemoryContentReciever reciever = new MemoryContentReciever();
			Frame pack = new Frame(input, reciever, b);
			input.done(b, 0, 0);

			if (!"GATEWAY/1.0".equals(pack.protocol())) {
				CJSystem.logging().error(getClass(), "不是网关协议侦:" + pack.protocol());
				return;
			}
			switch (pack.command()) {
			case "heartbeat":
				return;
			case "frame":
				doFramePack(ctx, pack);
				break;
			case "content":
				doContentPack(ctx, pack);
				break;
			case "last":
				doLastPack(ctx, pack);
				break;
			default:
				throw new EcmException("不支持的gateway指令：" + pack.command());
			}

		}

		private void doFramePack(ChannelHandlerContext ctx, Frame pack) throws Exception {
			TcpInputChannel input = new TcpInputChannel();
			Frame frame = input.begin(pack);
			IOutputChannel output = new TcpOutputChannel(ctx.channel(), frame);
			Circuit circuit = new HttpCircuit(output, String.format("%s 200 OK", frame.protocol()));
			this.currentCircuit = circuit;
			this.inputChannel = input;

			String uri = frame.url();
			String gatewayDestInHeader = frame.head(__frame_gatewayDest);
			String gatewayDest = GetwayDestHelper.getGatewayDestForHttpRequest(uri, gatewayDestInHeader, getClass());
			if (StringUtil.isEmpty(gatewayDest) || gatewayDest.endsWith("://")) {
				if ("error".equals(frame.command()) && "GATEWAY/1.0".equals(frame.protocol())) {
					String acceptErrorPath = (String) parent.getService("$.prop.acceptErrorPath");
					frame.url(acceptErrorPath);
					while (acceptErrorPath.startsWith("/")) {
						acceptErrorPath = acceptErrorPath.substring(1, acceptErrorPath.length());
					}
					int pos = acceptErrorPath.indexOf("/");
					if (pos > -1) {
						gatewayDest = acceptErrorPath.substring(0, pos);
					}else {
						gatewayDest=acceptErrorPath;
					}
				}
				if (StringUtil.isEmpty(gatewayDest)) {
					throw new CircuitException("404", "缺少路由目标，请求侦被丢掉：" + uri);
				}
			}

			IInputPipeline inputPipeline = pipelines.get(gatewayDest);
			// 检查目标管道是否存在
			if (inputPipeline != null) {
				flowPipeline(inputPipeline, ctx, frame, circuit);
				return;
			}

			// 以下生成目标管道
			inputPipeline=pipelineBuild(gatewayDest,  circuit, ctx);
			flowPipeline(inputPipeline, ctx, frame, circuit);// 再把本次请求发送处理
		}

		private void doContentPack(ChannelHandlerContext ctx, Frame pack) throws Exception {
			if (inputChannel == null) {
				return;
			}

			byte[] b = pack.content().readFully();
			Circuit circuit = this.currentCircuit;
			try {
				inputChannel.writeBytes(b, 0, b.length);
			} catch (Exception e) {
				if (!circuit.content().isCommited()) {
					circuit.status("503");
					circuit.message(e.toString().replace("\r", "").replace("\n", ""));
					circuit.content().clearbuf();
					circuit.content().flush();
				}
				StringWriter out = new StringWriter();
				e.printStackTrace(new PrintWriter(out));
				circuit.content().writeBytes(out.toString().getBytes());
				throw e;
			}
		}

		private void doLastPack(ChannelHandlerContext ctx, Frame pack) throws Exception {
			if (inputChannel == null) {
				return;
			}

			byte[] b = pack.content().readFully();
			Circuit circuit = this.currentCircuit;
			try {
				inputChannel.done(b, 0, b.length);
				circuit.content().flush();// 到此刷新
			} catch (Exception e) {
				if (!circuit.content().isCommited()) {
					circuit.status("503");
					circuit.message(e.toString().replace("\r", "").replace("\n", ""));
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
				this.currentCircuit = null;
				this.inputChannel = null;
			}
		}

		protected void flowPipeline(IInputPipeline pipeline, ChannelHandlerContext ctx, Frame frame, Circuit circuit)
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
			}
		}

		protected IInputPipeline pipelineBuild(String gatewayDest,  Circuit circuit, ChannelHandlerContext ctx)
				throws Exception {

			IGatewaySocket socket = this.sockets.getAndCreate(gatewayDest);
			String pipelineName = SocketName.name(ctx.channel().id(), gatewayDest);
			IInputPipelineBuilder builder = (IInputPipelineBuilder) socket.getService("$.pipeline.input.builder");
			IInputPipeline inputPipeline = builder.name(pipelineName).prop(__pipeline_fromProtocol, "tcp")
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
		public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
			if (evt instanceof IdleStateEvent) {
				// 不管是读事件空闲还是写事件空闲都向服务器发送心跳包
				sendHeartbeatPacket(ctx);
			} else {
				super.userEventTriggered(ctx, evt);
			}
		}

		private void sendHeartbeatPacket(ChannelHandlerContext ctx) throws CircuitException {
			IInputChannel input = new SimpleInputChannel();
			Frame f = new Frame(input, "heartbeat / gateway/1.0");
			byte[] box = TcpFrameBox.box(f.toBytes());
			ByteBuf bb = Unpooled.buffer();
			bb.writeBytes(box);
			ctx.writeAndFlush(bb);

		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			super.exceptionCaught(ctx, cause);
		}
	}
}
