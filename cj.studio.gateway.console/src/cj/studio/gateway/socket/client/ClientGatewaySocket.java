package cj.studio.gateway.socket.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.IGatewaySocketContainer;
import cj.studio.gateway.socket.Destination;
import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.cable.GatewaySocketCable;
import cj.studio.gateway.socket.cable.IGatewaySocketCable;
import cj.studio.gateway.socket.client.pipeline.builder.ClientSocketInputPipelineBuilder;
import cj.ultimate.util.StringUtil;

//管理电缆集合
public class ClientGatewaySocket implements IGatewaySocket {
	IServiceProvider parent;
	List<IGatewaySocketCable> cables;// 电览集合,一个uri一个电缆
	private Destination destination;
	private boolean isConnected;
	IExecutorPool pool;

	public ClientGatewaySocket(IServiceProvider parent) {
		this.parent = parent;
		cables = new CopyOnWriteArrayList<>();
		pool = new ExecutorPool();
	}

	@Override
	public Object getService(String name) {
		if ("$.pipeline.input.builder".equals(name)) {
			return  new ClientSocketInputPipelineBuilder(this);
		}
		if ("$.socket".equals(name)) {
			return this;
		}
		if ("$.socket.name".equals(name)) {
			return this.name();
		}
		if ("$.destination".equals(name)) {
			return destination;
		}
		if ("$.cables".equals(name)) {
			return cables;
		}
		if ("$.executor.pool".equals(name)) {
			return pool;
		}
		if ("$.socket.loopsize".equals(name)) {
			return this.pool.count();
		}
		return parent.getService(name);
	}

	@Override
	public <T> ServiceCollection<T> getServices(Class<T> clazz) {
		return parent.getServices(clazz);
	}

	@Override
	public String name() {
		return destination.getName();
	}

	@Override
	public void connect(Destination dest) throws CircuitException {
		if (isConnected) {
			return;
		}
		this.destination = dest;

		String broadcast = dest.getProps().get("Broadcast-Mode");
		if (StringUtil.isEmpty(broadcast)) {
			broadcast = "unicast";// multicast
			dest.getProps().put("Broadcast-Mode", broadcast);
		}

		List<String> uris = dest.getUris();
		// 控制台配的目标属性是所有电缆共享的，如总广播策略等；而对于目标内的每个uri由于其属性都可能不同，因此按以下连接串的方式配置
		for (String connStr : uris) {// uri表示为connStr：
										// protocol://ip:port?workThreadCount=4&initialWireSize=4&acquireRetryAttempts=10&xx=...
			IGatewaySocketCable cable = new GatewaySocketCable(this);
			cable.init(connStr);
			switch (cable.protocol()) {
			case "http":
			case "https":
				pool.requestHttpThreadCount(cable.workThreadCount());
				break;
			case "tcp":
				pool.requestTcpThreadCount(cable.workThreadCount());
				break;
			case "ws":
				pool.requestWsThreadCount(cable.workThreadCount());
				break;
			case "udt":
				pool.requestUdtThreadCount(cable.workThreadCount());
				break;
			default:
				throw new CircuitException("505", "不支持的协议：" + cable.protocol());
			}
			cables.add(cable);
		}
		pool.ready();

		dest.getProps().put("workThreadCount", pool.count() + "");

		for (IGatewaySocketCable cable : cables) {
			cable.connect();
		}

		isConnected = true;
	}

	@Override
	public void close() throws CircuitException {
		IGatewaySocketContainer container = (IGatewaySocketContainer) parent.getService("$.container.socket");
		if (container != null) {
			container.remove(name());
		}
		for (IGatewaySocketCable cable : cables) {
			cable.dispose();
		}
		cables.clear();
		pool.shutdown();
		parent = null;
		isConnected = false;

	}

}
