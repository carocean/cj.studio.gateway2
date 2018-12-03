package cj.studio.gateway.socket.cable.wire;

import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.cable.IGatewaySocketWire;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.AttributeKey;

public class HttpGatewaySocketWire implements IGatewaySocketWire {
	Channel channel;
	IServiceProvider parent;
	volatile boolean isIdle;
	private long idleBeginTime;

	public HttpGatewaySocketWire(IServiceProvider parent) {
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
		DefaultFullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.valueOf(frame.protocol()),
				HttpMethod.valueOf(frame.command()), frame.url());
		if (frame.content().readableBytes() > 0) {
			req.content().writeBytes(frame.content().readFully());
			req.headers().set(HttpHeaders.Names.CONTENT_LENGTH, frame.content().readableBytes());
		}
		String[] names = frame.enumHeadName();
		for (String name : names) {
			if ("command".equals(name) || "method".equals(name) || "protocol".equals(name) || "url".equals(name)) {
				continue;
			}
			String v = frame.head(name);
			req.headers().add(name, v);
		}
		if (!req.headers().contains("Host")) {// Host是http协议必须的，否则无响应
			String host = String.format("%s:%s", parent.getService("$.prop.host"), parent.getService("$.prop.port"));
			req.headers().add(HttpHeaders.Names.HOST, host);
		}
		if (!req.headers().contains(HttpHeaders.Names.CONNECTION)) {
			req.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		}
		ChannelPromise promise = channel.writeAndFlush(req).channel().newPromise();

		AttributeKey<ChannelPromise> promiseKey = AttributeKey.valueOf("Channel-Promise");
		AttributeKey<Circuit> circuitKey = AttributeKey.valueOf("Http-Circuit");
		promise.channel().attr(circuitKey).set((Circuit) response);
		promise.channel().attr(promiseKey).set(promise);
		try {
			long requestTimeout = (long) parent.getService("$.prop.requestTimeout");
			if (!promise.await(requestTimeout, TimeUnit.MILLISECONDS)) {
				throw new CircuitException("505", "请求超时：" + frame);
			}
		} catch (InterruptedException e) {
			throw new CircuitException("800", e);
		}
		return response;
	}

	@Override
	public synchronized void connect(String ip, int port) throws CircuitException {
		EventLoopGroup group = (EventLoopGroup) parent.getService("$.eventloop.group");
		Bootstrap b = new Bootstrap();
		b.group(group).channel(NioSocketChannel.class).option(ChannelOption.SO_KEEPALIVE, true)
//        .option(ChannelOption.SO_TIMEOUT, this.timeout)
				.handler(new HttpClientGatewaySocketInitializer());
		try {
			this.channel = b.connect(ip, port).sync().channel();
			channel.closeFuture();
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

	class HttpClientGatewaySocketInitializer extends ChannelInitializer<SocketChannel> {

		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			ChannelPipeline pipeline = ch.pipeline();
			// 客户端接收到的是httpResponse响应，所以要使用HttpResponseDecoder进行解码
//			pipeline.addLast(new HttpResponseDecoder());
			// 客户端发送的是httprequest，所以要使用HttpRequestEncoder进行编码
//			pipeline.addLast(new HttpRequestEncoder());
//			int aggregatorLimit=(int)parent.getService("$.prop.aggregatorLimit");

			pipeline.addLast("http-codec", new HttpClientCodec());
//			ch.pipeline().addLast("aggregator", new HttpObjectAggregator(aggregatorLimit));
			pipeline.addLast(new HttpClientGatewaySocketHandler());

		}

	}

	class HttpClientGatewaySocketHandler extends SimpleChannelInboundHandler<Object> {
		@Override
		protected void messageReceived(ChannelHandlerContext ctx, Object response) throws Exception {
//			if (!(response instanceof DefaultFullHttpResponse)) {// 由于使用了HttpObjectAggregator，所以一定是DefaultFullHttpResponse
//				return;
//			}

//			DefaultFullHttpResponse res = (DefaultFullHttpResponse) response;
			AttributeKey<Circuit> circuitKey = AttributeKey.valueOf("Http-Circuit");
			Circuit circuit = ctx.channel().attr(circuitKey).get();

			if (response instanceof LastHttpContent) {
				LastHttpContent last = (LastHttpContent) response;
				if (circuit.hasFeedback()) {
					circuit.doneFeeds(last.content());
				} else {
					if (last.content().readableBytes() > 0) {
						circuit.content().writeBytes(last.content());
					}
				}
				AttributeKey<ChannelPromise> promiseKey = AttributeKey.valueOf("Channel-Promise");
				ChannelPromise promise = ctx.channel().attr(promiseKey).get();
				promise.setSuccess();
				return;
			}
			if (response instanceof HttpResponse) {
				HttpResponse res = (HttpResponse) response;
				List<Entry<String, String>> list = res.headers().entries();
				for (Entry<String, String> en : list) {
					circuit.head(en.getKey(), en.getValue());
				}
				if (circuit.hasFeedback()) {
					circuit.beginFeeds();
				}
				return;
			}
			if (response instanceof HttpContent) {
				HttpContent content = (HttpContent) response;
				if (circuit.hasFeedback()) {
					circuit.writeFeeds(content.content());
				} else {
					if (content.content().readableBytes() > 0) {
						circuit.content().writeBytes(content.content());
					}
				}
				return;
			}

		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			@SuppressWarnings("unchecked")
			List<IGatewaySocketWire> wires = (List<IGatewaySocketWire>) parent.getService("$.wires");
			wires.remove(HttpGatewaySocketWire.this);
			super.channelInactive(ctx);
		}
	}

}
