package cj.studio.gateway.socket.cable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.socket.cable.wire.HttpGatewaySocketWire;
import cj.studio.gateway.socket.cable.wire.TcpGatewaySocketWire;
import cj.studio.gateway.socket.cable.wire.UdtGatewaySocketWire;
import cj.studio.gateway.socket.cable.wire.WSGatewaySocketWire;
import cj.studio.gateway.socket.util.SocketContants;
import cj.ultimate.util.StringUtil;

public class GatewaySocketCable implements IGatewaySocketCable, IServiceProvider {
	private IServiceProvider parent;
	private List<IGatewaySocketWire> wires;
	private ThreadLocal<IGatewaySocketWire> local;// 每个用户线程一根导线。因为netty对端的server的线程数是固定的，所以不必实现连接池，连接池由于有同步锁机制肯定不是性能最优。
	private long maxIdleTime;
	private int initialWireSize;
	private String host;
	private int port;
	private String protocol;
	boolean isOpened;
	private String wspath;
	private long heartbeat;
	private int workThreadCount;
	private int maxIdleConnections;
	private long keepAliveDuration;
	private long connectTimeout;
	private long readTimeout;
	private long writeTimeout;
	private boolean followRedirects;
	private boolean retryOnConnectionFailure;
	private int aggregatorLimit;
	private String acceptErrorPath;// 客户端接收系统error的应用处理地址，让开发者指定将错误导航到哪个website的什么webview中接收处理，在tcp|udt中有效
	private String onChannelEvent_Notify_Dests;

	public GatewaySocketCable(IServiceProvider parent) {
		this.parent = parent;
		wires = new CopyOnWriteArrayList<>();
		this.local = new ThreadLocal<>();
	}

	@Override
	public int initialWireSize() {
		return initialWireSize;
	}

	@Override
	public int workThreadCount() {
		return workThreadCount;
	}

	@Override
	public String host() {
		return host;
	}

	@Override
	public int port() {
		return port;
	}

	@Override
	public String protocol() {
		return protocol;
	}

	@Override
	public long getHeartbeat() {
		return heartbeat;
	}

	@Override
	public Object getService(String name) {
		if ("$.wires".equals(name)) {
			return wires;
		}
		if ("$.wires.count".equals(name)) {
			return wires.size();
		}
		if ("$.prop.host".equals(name)) {
			return host;
		}
		if ("$.prop.port".equals(name)) {
			return port;
		}
		if ("$.prop.protocol".equals(name)) {
			return protocol;
		}
		if ("$.prop.heartbeat".equals(name)) {
			return heartbeat;
		}
		if ("$.prop.maxIdleConnections".equals(name)) {
			return maxIdleConnections;
		}
		if ("$.prop.keepAliveDuration".equals(name)) {
			return keepAliveDuration;
		}
		if ("$.prop.connectTimeout".equals(name)) {
			return connectTimeout;
		}
		if ("$.prop.readTimeout".equals(name)) {
			return readTimeout;
		}
		if ("$.prop.writeTimeout".equals(name)) {
			return writeTimeout;
		}
		if ("$.prop.followRedirects".equals(name)) {
			return followRedirects;
		}
		if ("$.prop.retryOnConnectionFailure".equals(name)) {
			return retryOnConnectionFailure;
		}
		if ("$.prop.wspath".equals(name)) {
			return wspath;
		}
		if ("$.prop.aggregatorLimit".equals(name)) {
			return aggregatorLimit;
		}
		if ("$.prop.acceptErrorPath".equals(name)) {
			return this.acceptErrorPath;
		}
		if ("$.prop.OnChannelEvent-Notify-Dests".equals(name)) {
			return onChannelEvent_Notify_Dests;
		}
		return parent.getService(name);
	}

	@Override
	public <T> ServiceCollection<T> getServices(Class<T> clazz) {
		return parent.getServices(clazz);
	}

	@Override
	public IGatewaySocketWire select() throws CircuitException {
		// 选择导线后，导线为忙
		IGatewaySocketWire wire = local.get();
		if (wire != null && wire.isOpened()) {
			return wire;
		}
		for (IGatewaySocketWire w : wires) {
			if (w == null)
				continue;
			if (w.isIdle() && w.isOpened()) {
				w.used(true);
				local.set(w);
				return w;
			}
		}

		checkWires();
		// 以下是新建
		wire = createWire();
		wire.connect(host, port);
		wire.used(true);
		local.set(wire);
		return wire;
	}

	private void checkWires() {
		for (IGatewaySocketWire w : wires) {
			if (w == null)
				continue;
			if (!w.isOpened()) {
				wires.remove(w);
				continue;
			}
			if ((System.currentTimeMillis() - w.idleBeginTime()) > this.maxIdleTime) {
				w.dispose();// 释放连接，物理关闭
				wires.remove(w);
				continue;
			}
		}

	}

	@Override
	public void close() {
		for (IGatewaySocketWire w : wires) {
			if (w == null)
				continue;
			w.close();
		}
		wires.clear();
		isOpened = false;
	}

	@Override
	public void dispose() {
		for (IGatewaySocketWire w : wires) {
			if (w == null)
				continue;
			w.dispose();
		}
		wires.clear();
		isOpened = false;
	}

	@Override
	public void init(String connStr) throws CircuitException {
		parseConnStr(connStr);
	}

	@Override
	public void connect() throws CircuitException {
		if (isOpened)
			return;
		onconnect();
		isOpened = true;
	}

	private void onconnect() throws CircuitException {
		for (int i = 0; i < this.initialWireSize; i++) {
			IGatewaySocketWire wire = createWire();
			wire.connect(host, port);
		}
	}

	protected IGatewaySocketWire createWire() throws CircuitException {
		IGatewaySocketWire wire = null;
		switch (protocol) {
		case "http":
		case "https":
			wire = new HttpGatewaySocketWire(this);
			break;
		case "ws":
			wire = new WSGatewaySocketWire(this);
			break;
		case "tcp":
			wire = new TcpGatewaySocketWire(this);
			break;
		case "udt":
			wire = new UdtGatewaySocketWire(this);
			break;
		default:
			throw new CircuitException("505", "不支持的协议:" + protocol);
		}
		wires.add(wire);
		return wire;
	}

	private void parseConnStr(String connStr) {
		int pos = connStr.indexOf("://");
		protocol = connStr.substring(0, pos);
		String uri = connStr.substring(pos + 3, connStr.length());
		pos = uri.indexOf("?");
		String address = "";
		String q = "";
		if (pos < 0) {
			address = uri;
			q = "";
		} else {
			address = uri.substring(0, pos);
			q = uri.substring(pos + 1, uri.length());
		}
		while (address.endsWith("/")) {
			address = address.substring(0, address.length() - 1);
		}
		String[] addressArr = address.split(":");
		if (addressArr.length < 2) {
			this.host = addressArr[0];
			this.port = 80;
		} else {
			this.host = addressArr[0];
			this.port = Integer.valueOf(addressArr[1]);
		}

		Frame f = new Frame(null, String.format("parse /?%s %s/1.0", q, protocol));

		this.workThreadCount = StringUtil.isEmpty(f.parameter("workThreadCount"))
				? Runtime.getRuntime().availableProcessors() * 2
				: Integer.valueOf(f.parameter("workThreadCount"));
		this.initialWireSize = StringUtil.isEmpty(f.parameter("initialWireSize")) ? 1
				: Integer.valueOf(f.parameter("initialWireSize"));
		this.heartbeat = StringUtil.isEmpty(f.parameter("heartbeat")) ? -1 : Long.valueOf(f.parameter("heartbeat"));
		this.maxIdleTime = StringUtil.isEmpty(f.parameter("maxIdleTime")) ? 300000L
				: Long.valueOf(f.parameter("maxIdleTime"));
		this.maxIdleConnections = StringUtil.isEmpty(f.parameter("maxIdleConnections")) ? 5
				: Integer.valueOf(f.parameter("maxIdleConnections"));
		this.keepAliveDuration = StringUtil.isEmpty(f.parameter("keepAliveDuration")) ? 300000L
				: Long.valueOf(f.parameter("keepAliveDuration"));
		this.connectTimeout = StringUtil.isEmpty(f.parameter("connectTimeout")) ? 10000L
				: Long.valueOf(f.parameter("connectTimeout"));
		this.readTimeout = StringUtil.isEmpty(f.parameter("readTimeout")) ? 10000L
				: Long.valueOf(f.parameter("readTimeout"));
		this.writeTimeout = StringUtil.isEmpty(f.parameter("writeTimeout")) ? 10000L
				: Long.valueOf(f.parameter("writeTimeout"));
		this.followRedirects = StringUtil.isEmpty(f.parameter("followRedirects")) ? true
				: Boolean.valueOf(f.parameter("followRedirects"));
		this.retryOnConnectionFailure = StringUtil.isEmpty(f.parameter("retryOnConnectionFailure")) ? true
				: Boolean.valueOf(f.parameter("retryOnConnectionFailure"));
		this.aggregatorLimit = StringUtil.isEmpty(f.parameter("aggregatorLimit")) ? 1024 * 1024 * 2
				: Integer.valueOf(f.parameter("aggregatorLimit"));
		this.acceptErrorPath = StringUtil.isEmpty(f.parameter("acceptErrorPath")) ? "/error/"
				: f.parameter("acceptErrorPath");
		this.onChannelEvent_Notify_Dests = StringUtil
				.isEmpty(f.parameter(SocketContants.__channel_onchannelEvent_notify_dests)) ? ""
						: f.parameter(SocketContants.__channel_onchannelEvent_notify_dests);
		if ("ws".equals(protocol)) {
			wspath = f.parameter("wspath");
			if (StringUtil.isEmpty(wspath)) {
				throw new EcmException("没有为ws协议指定请求目标地址，指在连结串中设置wspath参数");
			}
		}
		f.dispose();
	}

}
