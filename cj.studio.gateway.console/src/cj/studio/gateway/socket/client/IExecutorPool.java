package cj.studio.gateway.socket.client;

import java.util.concurrent.ExecutorService;

import io.netty.channel.EventLoopGroup;

public interface IExecutorPool {

	void requestHttpThreadCount(int workThreadCount);

	void requestTcpThreadCount(int workThreadCount);

	void requestWsThreadCount(int workThreadCount);

	void requestUdtThreadCount(int workThreadCount);

	EventLoopGroup getEventLoopGroup();

	EventLoopGroup getEventLoopGroup_udt();

	ExecutorService getExecutor();

	void ready();

	int count();

	void shutdown();

}
