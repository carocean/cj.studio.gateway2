package cj.studio.gateway.socket.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.net.nio.netty.udt.UtilThreadFactory;
import cj.studio.gateway.socket.Destination;
import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.cable.GatewaySocketCable;
import cj.studio.gateway.socket.cable.IGatewaySocketCable;
import cj.studio.gateway.socket.client.pipeline.builder.ClientSocketInputPipelineBuilder;
import cj.studio.gateway.socket.pipeline.IInputPipelineBuilder;
import cj.ultimate.util.StringUtil;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.udt.nio.NioUdtProvider;

//管理电缆集合
public class ClientGatewaySocket implements IGatewaySocket {
	IServiceProvider parent;
	List<IGatewaySocketCable> cables;// 电览集合,一个uri一个电缆
	private Destination destination;
	private IInputPipelineBuilder inputBuilder;// ClientGatewaySocket不需要输出管道,这与server端向目标端的输出一致。
	private boolean isConnected;
	EventLoopGroup eventloopGroup;
	EventLoopGroup eventloopGroup_Udt;

	public ClientGatewaySocket(IServiceProvider parent) {
		this.parent = parent;
		cables = new ArrayList<>();
		inputBuilder = new ClientSocketInputPipelineBuilder(this);
	}

	@Override
	public Object getService(String name) {
		if ("$.pipeline.input.builder".equals(name)) {
			return inputBuilder;
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
		if ("$.eventloop.group".equals(name)) {
			return eventloopGroup;
		}
		if ("$.eventloop.group.udt".endsWith(name)) {
			return eventloopGroup_Udt;
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
		int nThread = 0;
		int nUdtThread = 0;
		for (String connStr : uris) {// uri表示为connStr： protocol://ip:port?minWireSize=4&maxWireSize=10&xx=...
			IGatewaySocketCable cable = new GatewaySocketCable(this);
			cable.init(connStr);
			if ("udt".equals(cable.protocol())) {
				nUdtThread += cable.maxWireSize();
			} else {
				nThread += cable.maxWireSize();
			}
			cables.add(cable);
		}
		nThread += 1;// 多给一个线程
		
		String workThreadCount = dest.getProps().get("workThreadCount");
		if (StringUtil.isEmpty(workThreadCount)) {
			workThreadCount = "1";
		}
		int userNThread = Integer.valueOf(workThreadCount);
		if (userNThread < nThread) {
			CJSystem.logging().warn(getClass(),
					String.format("配置的线程：%s 小于电缆需要的线程数:%s，系统为之分配为：%s", userNThread, nThread, nThread));
		} else {
			nThread = userNThread;
		}
		dest.getProps().put("workThreadCount", nThread + "");
		this.eventloopGroup = new NioEventLoopGroup(nThread);
		if(nUdtThread>0) {
			ThreadFactory connectFactory = new UtilThreadFactory("udt_wire");
			this.eventloopGroup_Udt=new NioEventLoopGroup(nUdtThread, connectFactory, NioUdtProvider.MESSAGE_PROVIDER);
		}
		for (IGatewaySocketCable cable : cables) {
			cable.connect();
		}

		isConnected = true;
	}

	@Override
	public void close() throws CircuitException {
		for (IGatewaySocketCable cable : cables) {
			cable.close();
		}
		cables.clear();
		parent = null;
		isConnected = false;
		this.inputBuilder = null;
	}

}
