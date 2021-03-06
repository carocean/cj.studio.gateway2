package cj.studio.gateway.socket.cable.wire;

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
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.ecm.net.io.MemoryInputChannel;
import cj.studio.ecm.net.io.SimpleInputChannel;
import cj.studio.gateway.IGatewaySocketContainer;
import cj.studio.gateway.IJunctionTable;
import cj.studio.gateway.junction.BackwardJunction;
import cj.studio.gateway.junction.Junction;
import cj.studio.gateway.server.util.GetwayDestHelper;
import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.cable.IGatewaySocketWire;
import cj.studio.gateway.socket.cable.wire.reciever.UdtContentReciever;
import cj.studio.gateway.socket.client.IExecutorPool;
import cj.studio.gateway.socket.io.UdtInputChannel;
import cj.studio.gateway.socket.io.UdtOutputChannel;
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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.udt.UdtChannel;
import io.netty.channel.udt.UdtMessage;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

public class UdtGatewaySocketWire implements IGatewaySocketWire {
	Channel channel;
	IServiceProvider parent;
	volatile boolean isIdle;
	private long idleBeginTime;

	public UdtGatewaySocketWire(IServiceProvider parent) {
		this.parent = parent;
		used(false);
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
	public void updateIdleBeginTime() {
		idleBeginTime=System.currentTimeMillis();
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
		Frame frame = (Frame) request;
		byte[] b=null;
		boolean mustBeDone = false;
		if(frame.content().hasReciever()) {
			if(!frame.content().isAllInMemory()) {
				throw new CircuitException("503", "UDT仅支持MemoryContentReciever或者内容接收器为空." + frame);
			}
			if (frame.content().revcievedBytes() > 0) {
				b = frame.content().readFully();
			}else {
				b=new byte[0];
			}
			mustBeDone=true;
		}
		UdtContentReciever tcr = new UdtContentReciever(channel);
		frame.content().accept(tcr);

		MemoryInputChannel in = new MemoryInputChannel(8192);
		Frame pack = new Frame(in, "frame / gateway/1.0");// 有三种包：frame,content,last。frame包无内容；content和last包有内容无头
		pack.content().accept(new MemoryContentReciever());
		in.begin(null);
		byte[] data = frame.toBytes();
		in.done(data, 0, data.length);

		UdtMessage msg = new UdtMessage(pack.toByteBuf());
		/* ChannelFuture future = */channel.writeAndFlush(msg);
//		try {
//			future.await(SocketContants.__channel_write_await_timeout, TimeUnit.MILLISECONDS);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		if (mustBeDone) {// 由于调用者使用了isAllInMemory接收器，前面在使用readFully()方法读取时如果没有完成则报流未完成异常，故而在使用内容的内存接收器模式时，发送前必须完成输入流，故而此处必须tcr.done
			tcr.done(b, 0, b.length);
		}
		updateIdleBeginTime();
		return null;
	}

	@Override
	public void connect(String ip, int port) throws CircuitException {
		IExecutorPool exepool = (IExecutorPool) parent.getService("$.executor.pool");
		EventLoopGroup group = exepool.getEventLoopGroup_udt();

		Bootstrap b = new Bootstrap();

		b.group(group).channelFactory(NioUdtProvider.MESSAGE_CONNECTOR)
				.handler(new UdtClientGatewaySocketInitializer());
		try {
			this.channel = b.connect(ip, port).sync().channel();
		} catch (Throwable e) {
			@SuppressWarnings("unchecked")
			List<IGatewaySocketWire> wires = (List<IGatewaySocketWire>) parent.getService("$.wires");
			wires.remove(this);
			throw new CircuitException("505", e);
		}
		updateIdleBeginTime();
	}

	@Override
	public boolean isWritable() {
		if(channel==null)return false;
		return channel.isWritable();
	}

	@Override
	public boolean isOpened() {
		if(channel==null)return false;
		return channel.isOpen();
	}

	class UdtClientGatewaySocketInitializer extends ChannelInitializer<UdtChannel> {

		@Override
		protected void initChannel(UdtChannel ch) throws Exception {
			ChannelPipeline pipeline = ch.pipeline();
			long interval = (long) parent.getService("$.prop.heartbeat");
			if (interval > 0) {
				pipeline.addLast(new IdleStateHandler(0, 0, interval, TimeUnit.MILLISECONDS));
			}
			pipeline.addLast(new UdtClientHandler());

		}

	}

	class UdtClientHandler extends SimpleChannelInboundHandler<UdtMessage> implements SocketContants {
		// 直接搜到目标的input传入即可,因此clientsocket不需要输出管道，性能更好。
		// 这与server端向目标端的输出一致。
		IGatewaySocketContainer sockets;
		private IJunctionTable junctions;
		InputPipelineCollection pipelines;
		private String socketName;
		private Circuit currentCircuit;
		private IInputChannel inputChannel;

		public UdtClientHandler() {
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
				Frame frame = new Frame(String.format("onactive /%s/ udt/1.0", gatewayDest));
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

			UdtGatewaySocketWire.this.close();
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
			UdtMessage msg = new UdtMessage(f.toByteBuf());
			ctx.writeAndFlush(msg);
		}

		@Override
		public void messageReceived(ChannelHandlerContext ctx, UdtMessage msg) throws Exception {

			ByteBuf bb = msg.content();
			if (bb.readableBytes() == 0) {
				return;
			}
			byte[] b = new byte[bb.readableBytes()];
			bb.readBytes(b);
			IInputChannel input = new MemoryInputChannel(8192);
			MemoryContentReciever reciever = new MemoryContentReciever();
			Frame pack = new Frame(input,reciever, b);
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
			UdtInputChannel input = new UdtInputChannel();
			Frame frame = input.begin(pack);
			IOutputChannel output = new UdtOutputChannel(ctx.channel(), frame);
			Circuit circuit = new DefaultSegmentCircuit(output, String.format("%s 200 OK", frame.protocol()));
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
			inputPipeline= pipelineBuild(gatewayDest,  circuit, ctx);
			flowPipeline(inputPipeline, ctx, frame, circuit);// 再把本次请求发送处理
		}

		private void doContentPack(ChannelHandlerContext ctx, Frame pack) throws Exception {
			if (inputChannel == null) {
				return;
			}

			byte[] b = pack.content().readFully();
			try {
				inputChannel.writeBytes(b, 0, b.length);
			} catch (Exception e) {
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
				throw e;
			}
		}

		protected IInputPipeline pipelineBuild(String gatewayDest, Circuit circuit, ChannelHandlerContext ctx)
				throws Exception {
			IGatewaySocket socket = this.sockets.getAndCreate(gatewayDest);

			IInputPipelineBuilder builder = (IInputPipelineBuilder) socket.getService("$.pipeline.input.builder");
			String pipelineName = SocketName.name(ctx.channel().id(), gatewayDest);
			IInputPipeline inputPipeline = builder.name(pipelineName).prop(__pipeline_fromProtocol, "udt")
					.prop(__pipeline_fromWho, socketName).prop(__pipeline_fromNetType, "client").createPipeline();
			pipelines.add(gatewayDest, inputPipeline);

			BackwardJunction junction = new BackwardJunction(pipelineName);
			junction.parse(inputPipeline, ctx.channel(), socket);
			this.junctions.add(junction);

			try {
				inputPipeline.headOnActive(pipelineName);// 通知管道激活
			} catch (Exception e) {
				circuit.content().close();
				ctx.close();
				throw e;
			}
			return inputPipeline;
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			super.exceptionCaught(ctx, cause);
		}
	}
}
