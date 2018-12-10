package cj.studio.gateway.socket.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import cj.studio.ecm.net.nio.netty.udt.UtilThreadFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.udt.nio.NioUdtProvider;

class ExecutorPool implements IExecutorPool {
	private int httpThreadCount;
	private int tcpThreadCount;
	private int udtThreadCount;
	private int wsThreadCount;
	private EventLoopGroup eventloopGroup_tcp_ws;
	private EventLoopGroup eventloopGroup_Udt;
	private ExecutorService executorService_http;

	@Override
	public void shutdown() {
		if(eventloopGroup_tcp_ws!=null) {
			eventloopGroup_tcp_ws.shutdownGracefully();
		}
		if(eventloopGroup_Udt!=null) {
			eventloopGroup_Udt.shutdownGracefully();
		}
		if(executorService_http!=null) {
			executorService_http.shutdown();
		}
	}

	@Override
	public void requestHttpThreadCount(int workThreadCount) {
		httpThreadCount += workThreadCount;
	}

	@Override
	public void requestTcpThreadCount(int workThreadCount) {
		tcpThreadCount += workThreadCount;
	}

	@Override
	public void requestWsThreadCount(int workThreadCount) {
		wsThreadCount += workThreadCount;
	}

	@Override
	public void requestUdtThreadCount(int workThreadCount) {
		udtThreadCount += workThreadCount;
	}

	@Override
	public void ready() {
		int nettyThreadCount = this.tcpThreadCount + wsThreadCount;//tcp和ws共享线程池
		if (nettyThreadCount > 0) {
			EventLoopGroup eventloopGroup_tcp_ws = new NioEventLoopGroup(nettyThreadCount);
			this.eventloopGroup_tcp_ws=eventloopGroup_tcp_ws;
		}
		if (udtThreadCount > 0) {
			ThreadFactory connectFactory = new UtilThreadFactory("udt_wire");
			EventLoopGroup eventloopGroup_Udt = new NioEventLoopGroup(udtThreadCount, connectFactory,
					NioUdtProvider.MESSAGE_PROVIDER);
			this.eventloopGroup_Udt=eventloopGroup_Udt;
		}
		if (httpThreadCount > 0) {
			ExecutorService executorService_http = Executors.newFixedThreadPool(httpThreadCount);
			this.executorService_http=executorService_http;
		}

	}

	@Override
	public int count() {
		return this.httpThreadCount + tcpThreadCount + udtThreadCount + wsThreadCount;
	}

	@Override
	public EventLoopGroup getEventLoopGroup() {
		return this.eventloopGroup_tcp_ws;
	}

	@Override
	public EventLoopGroup getEventLoopGroup_udt() {
		return this.eventloopGroup_Udt;
	}

	@Override
	public ExecutorService getExecutor() {
		return this.executorService_http;
	}

}
