package cj.studio.gateway.server;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.ServiceCollection;
import cj.studio.gateway.IGatewayServer;
import cj.studio.gateway.conf.ServerInfo;
import cj.studio.gateway.road.http.HttpServerInitializer;
import cj.studio.gateway.server.initializer.HttpChannelInitializer;
import cj.studio.gateway.server.secure.ISSLEngineFactory;
import cj.studio.gateway.server.secure.SSLEngineFactory;
import cj.ultimate.util.StringUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class HttpGatewayServer implements IGatewayServer, IServiceProvider {
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private ServerInfo info;
	boolean isStarted;
	IServiceProvider parent;
	ISSLEngineFactory sslEngine;
	public HttpGatewayServer(IServiceProvider parent) {
		this.parent = parent;
	}

	@Override
	public Object getService(String name) {
		if ("$.server.info".equals(name)) {
			return info;
		}
		if ("$.server.name".equals(name)) {
			return this.netName();
		}
		if("$.server.sslEngine".equals(name)) {
			return sslEngine;
		}
		return parent.getService(name);
	}

	@Override
	public <T> ServiceCollection<T> getServices(Class<T> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void stop() {
		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
		isStarted = false;
	}

	@Override
	public void start(ServerInfo si) {
		if (isStarted) {
			throw new EcmException(String.format("服务器%s已启动", this.netName()));
		}
		this.info = si;
		this.sslEngine=new SSLEngineFactory(this);
		this.sslEngine.refresh();
		int bcnt = bossThreadCount();
		int wcnt = workThreadCount();
		if (bcnt == -1) {
			bossGroup = new NioEventLoopGroup(1);
		} else {
			bossGroup = new NioEventLoopGroup(bcnt);
		}
		if (wcnt == -1) {
			workerGroup = new NioEventLoopGroup();
		} else {
			workerGroup = new NioEventLoopGroup(wcnt);
		}
		ServerBootstrap b = new ServerBootstrap();

		try {
			if (StringUtil.isEmpty(info.getRoad())) {
				b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
						.childOption(ChannelOption.SO_KEEPALIVE, true).childHandler(createChannelInitializer());
			} else {
	            b.group(bossGroup, workerGroup)
	             .channel(NioServerSocketChannel.class)
	             .childHandler(new HttpServerInitializer(this));
	            //下面一行注掉原因：经测，在win10上netty报主机主动关闭连接错误，而在linux上不报该错。就是去掉这句，在linux工作正常
//	             .childOption(ChannelOption.AUTO_READ, false);//这个属性很关键，如果为true则浏览器一直悬停。
			}
			Channel ch = null;
			if ("localhost".equals(ip())) {
				ch = b.bind(port()).sync().channel();
			} else {
				ch = b.bind(ip(), port()).sync().channel();
			}
			ch.closeFuture();
			isStarted = true;
		} catch (InterruptedException e) {
			throw new EcmException(e);
		}
	}

	protected ChannelHandler createChannelInitializer() {
		return new HttpChannelInitializer(this);
	}

	public String ip() {
		String host = info.getHost();
		String ip = "";
		int pos = host.indexOf(":");
		if (pos <= 0) {
			ip = "localhost";
		} else {
			String[] arr = host.split(":");
			ip = arr[0];
		}
		return ip;
	}

	public int port() {
		String host = info.getHost();
		int port = 0;
		int pos = host.indexOf(":");
		if (pos < 0) {
			port = 80;
		} else {
			String[] arr = host.split(":");
			port = Integer.valueOf(arr[1]);
		}
		return port;
	}

	@Override
	public String netName() {
		return info.getName();
	}

	@Override
	public boolean isStarted() {
		return isStarted;
	}

	@Override
	public int bossThreadCount() {
		String cnt = info.getProps().get("bossThreadCount");
		return StringUtil.isEmpty(cnt) ? -1 : Integer.valueOf(cnt);
	}

	@Override
	public int activeBossCount() {
		return bossGroup.children().size();
	}

	@Override
	public int workThreadCount() {
		String cnt = info.getProps().get("workThreadCount");
		return StringUtil.isEmpty(cnt) ? -1 : Integer.valueOf(cnt);
	}

	@Override
	public int activeWorkCount() {
		return workerGroup.children().size();
	}

}
