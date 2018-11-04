package cj.studio.gateway;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IChip;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.IServiceSetter;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceSite;
import cj.studio.gateway.conf.ServerInfo;
import cj.studio.gateway.server.HttpGatewayServer;
import cj.studio.gateway.server.TcpGatewayServer;

@CjService(name = "gatewayServerContainer")
public class GatewayServerContainer implements IGatewayServerContainer,IServiceSetter,IServiceProvider {
	Map<String, IGatewayServer> servers;
	@CjServiceSite
	IServiceSite site;
	IServiceProvider parent;
	@Override
	public void setService(String arg0, Object service) {
		this.parent=(IServiceProvider)service;
	}
	@Override
	public Object getService(String name) {
		if("$.chipinfo".equals(name)) {
			return ((IChip)site.getService(IChip.class.getName())).info();
		}
		return parent.getService(name);
	}
	@Override
	public <T> ServiceCollection<T> getServices(Class<T> arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void startAll() {
		this.servers = new HashMap<>();
		IConfiguration conf=(IConfiguration)parent.getService("$.config");
		Set<String> names = conf.enumServerNames();
		for (String name : names) {
			ServerInfo si = conf.serverInfo(name);
			startServerWithOutFlush(si);
		}
	}

	@Override
	public void stopAll() {
		String[] set = servers.keySet().toArray(new String[0]);
		for (String name : set) {
			stopServerWithOutFlush(name);
		}

	}

	@Override
	public void startServer(ServerInfo item) {
		IConfiguration config=(IConfiguration)parent.getService("$.config");
		if (config.containsServiceName(item.getName())) {
			throw new EcmException(String.format("已存在服务器：%s", item.getName()));
		}
		config.addServerInfo(item);
		config.flushServers();
		// 下面启动服务器
		startServerWithOutFlush(item);
	}

	protected void startServerWithOutFlush(ServerInfo si) {
		IGatewayServer server = null;
		switch (si.getProtocol()) {
//		case "ws":
//			server = new WebsocketGatewayServer();
//			break;
		case "http":
			server=new HttpGatewayServer(this);
			break;
		case "tcp":
			server=new TcpGatewayServer();
			break;
//		case "udt":
//			server=new UdtGatewayServer();
//			break;
//		case "jms":
//			throw new EcmException(String.format("网关的服务器端不支持jms，原因网关不需要接收jms消息，网关使用jms只是用于将由其它网络协议收取到的消息通过jms协议转出网关。"));
//		default:
//			throw new EcmException(String.format("不支持的网络协议：%s", si.getProtocol()));
		}
		server.start(si);
		servers.put(server.netName(), server);
	}

	@Override
	public void stopServer(String name) {
		IConfiguration config=(IConfiguration)parent.getService("$.config");
		ServerInfo item = config.serverInfo(name);
		if (item == null) {
			throw new EcmException(String.format("要停止的服务器：%s 不存在", name));
		}
		config.removeServerInfo(name);
		config.flushServers();
		// 下面停止服务器
		stopServerWithOutFlush(name);
	}

	protected void stopServerWithOutFlush(String name) {
		IGatewayServer server = servers.get(name);
		server.stop();
		servers.remove(name);
	}

	@Override
	public IGatewayServer server(String name) {
		return servers.get(name);
	}

	@Override
	public Set<String> enumServerName() {
		return servers.keySet();
	}

	@Override
	public boolean containsServer(String name) {
		return servers.containsKey(name);
	}
}
