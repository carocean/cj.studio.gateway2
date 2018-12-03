package cj.studio.gateway.socket.cable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.cable.wire.HttpGatewaySocketWire;
import cj.studio.gateway.socket.cable.wire.TcpGatewaySocketWire;
import cj.studio.gateway.socket.cable.wire.UdtGatewaySocketWire;
import cj.studio.gateway.socket.cable.wire.WSGatewaySocketWire;
import cj.studio.gateway.socket.util.SocketContants;
import cj.ultimate.util.StringUtil;

public class GatewaySocketCable implements IGatewaySocketCable, IServiceProvider {
	private IServiceProvider parent;
	private List<IGatewaySocketWire> wires;
	private int acquireRetryAttempts;
	private long maxIdleTime;
	private int maxWireSize;
	private int minWireSize;
	private int initialWireSize;
	private long checkoutTimeout;
	private String host;
	private int port;
	private String protocol;
	boolean isOpened;
	ReentrantLock lock;
	Condition waitingForCreateWire;
	private long requestTimeout;
	private String wspath;
	private int heartbeat;
	public GatewaySocketCable(IServiceProvider parent) {
		this.parent = parent;
		wires = new CopyOnWriteArrayList<>();
		lock = new ReentrantLock();
		waitingForCreateWire = lock.newCondition();
	}


	@Override
	public long requestTimeout() {
		return requestTimeout;
	}

	@Override
	public int acquireRetryAttempts() {
		return acquireRetryAttempts;
	}

	@Override
	public long maxIdleTime() {
		return maxIdleTime;
	}

	@Override
	public int maxWireSize() {
		return maxWireSize;
	}

	@Override
	public int minWireSize() {
		return minWireSize;
	}

	@Override
	public int initialWireSize() {
		return initialWireSize;
	}

	@Override
	public long checkoutTimeout() {
		return checkoutTimeout;
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
	public int getHeartbeat() {
		return heartbeat;
	}
	@Override
	public Object getService(String name) {
		if ("$.wires".equals(name)) {
			return wires;
		}
		if ("$.waitingForCreateWire".equals(name)) {
			return waitingForCreateWire;
		}
		if ("$.lock".equals(name)) {
			return lock;
		}
		if ("$.prop.host".equals(name)) {
			return host;
		}
		if ("$.prop.port".equals(name)) {
			return port;
		}
		if ("$.prop.requestTimeout".equals(name)) {
			return requestTimeout;
		}
		if ("$.prop.wspath".equals(name)) {
			return wspath;
		}
		if ("$.wires.count".equals(name)) {
			return wires.size();
		}
		if(SocketContants.__key_heartbeat_interval.equals(name)) {
			return heartbeat;
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
		try {
			lock.lock();
			IGatewaySocketWire wire = selectInExists();
			if (wire != null) {
				wire.used(true);
				return wire;
			}
			// 需要新建wire
			if (wires.size() > this.maxWireSize) {// 等待有空闲的导线
				if (checkoutTimeout <= 0) {
					waitingForCreateWire.await();
				} else {
					boolean elapsed = waitingForCreateWire.await(checkoutTimeout, TimeUnit.MILLISECONDS);// 当wire的close会触发此条件
					if (!elapsed) {
						CJSystem.logging().error(getClass(), "waitingForCreateWire超时:" + checkoutTimeout);
					}
				}
				select();
				return null;
			}
			// 以下是新建
			wire = createWire();
			wire.connect(host, port);
			wire.used(true);
			wires.add(wire);
			checkWires();// 最后检查导线，如果存在多余空闲、不能写、没打开等情况，则移除它。
			return wire;
		} catch (Exception e) {
			throw new CircuitException("505", e);
		} finally {
			lock.unlock();
		}
	}

	private void checkWires() {
		IGatewaySocketWire[] arr = wires.toArray(new IGatewaySocketWire[0]);
		for (IGatewaySocketWire w : arr) {
			if (w == null)
				continue;
			if ((System.currentTimeMillis() - w.idleBeginTime()) > this.maxIdleTime) {
				w.dispose();// 释放连接，物理关闭
				wires.remove(w);
				continue;
			}
			if (!w.isOpened() || !w.isWritable()) {
				wires.remove(w);
				continue;
			}
		}

	}

	private synchronized IGatewaySocketWire selectInExists() {
		// 检查现有导线是否有空闲的
		for (IGatewaySocketWire wire : wires) {
			if (wire == null) {
				continue;
			}
			if (wire.isIdle() && wire.isWritable()) {
				return wire;
			}
		}
		return null;
	}

	@Override
	public void close() {
		IGatewaySocketWire [] arr=this.wires.toArray(new IGatewaySocketWire[0]);
		for(IGatewaySocketWire w:arr) {
			if(w==null)continue;
			w.close();
		}
		wires.clear();
		isOpened = false;
	}
	@Override
	public void dispose() {
		IGatewaySocketWire [] arr=this.wires.toArray(new IGatewaySocketWire[0]);
		for(IGatewaySocketWire w:arr) {
			if(w==null)continue;
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
		onOpen();
		isOpened = true;
	}

	private void onOpen() throws CircuitException {
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

		Frame f = new Frame(String.format("parse /?%s %s/1.0", q, protocol));

		this.acquireRetryAttempts = StringUtil.isEmpty(f.parameter("acquireRetryAttempts")) ? 10
				: Integer.valueOf(f.parameter("acquireRetryAttempts"));
		this.checkoutTimeout = StringUtil.isEmpty(f.parameter("checkoutTimeout")) ? 300000
				: Long.valueOf(f.parameter("checkoutTimeout"));
		this.initialWireSize = StringUtil.isEmpty(f.parameter("initialWireSize")) ? 1
				: Integer.valueOf(f.parameter("initialWireSize"));
		this.maxIdleTime = StringUtil.isEmpty(f.parameter("maxIdleTime")) ? 10000L
				: Long.valueOf(f.parameter("maxIdleTime"));
		this.requestTimeout = StringUtil.isEmpty(f.parameter("requestTimeout")) ? Long.MAX_VALUE
				: Long.valueOf(f.parameter("requestTimeout"));
		this.maxWireSize = StringUtil.isEmpty(f.parameter("maxWireSize")) ? 4
				: Integer.valueOf(f.parameter("maxWireSize"));
		this.minWireSize = StringUtil.isEmpty(f.parameter("minWireSize")) ? 2
				: Integer.valueOf(f.parameter("minWireSize"));
		this.heartbeat = StringUtil.isEmpty(f.parameter(SocketContants.__key_heartbeat_interval)) ? -1
				: Integer.valueOf(f.parameter(SocketContants.__key_heartbeat_interval));
		if (StringUtil.isEmpty(f.parameter("initialWireSize"))) {
			initialWireSize = minWireSize;
		} else {
			int i = Integer.valueOf(f.parameter("initialWireSize"));
			if (i < minWireSize || i > maxWireSize) {
				throw new EcmException(String.format(
						"initialWireSize取值必须在minWireSize与maxWireSize之间。initialWireSize=%s,minWireSize=%s,maxWireSize=%s",
						i, minWireSize, maxWireSize));
			}
			initialWireSize = i;
		}
		if ("ws".equals(protocol)) {
			wspath = f.parameter("wspath");
			if (StringUtil.isEmpty(wspath)) {
				throw new EcmException("没有为ws协议指定请求目标地址，指在连结串中设置wspath参数");
			}
		}
		f.dispose();
	}

}
