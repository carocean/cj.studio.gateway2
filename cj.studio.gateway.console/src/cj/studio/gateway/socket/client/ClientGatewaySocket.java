package cj.studio.gateway.socket.client;

import java.util.ArrayList;
import java.util.List;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.Destination;
import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.cable.GatewaySocketCable;
import cj.studio.gateway.socket.cable.IGatewaySocketCable;
import cj.studio.gateway.socket.client.pipeline.builder.ClientSocketInputPipelineBuilder;
import cj.studio.gateway.socket.client.pipeline.builder.ClientSocketOutputPipelineBuilder;
import cj.studio.gateway.socket.pipeline.IInputPipelineBuilder;
import cj.studio.gateway.socket.pipeline.IOutputPipelineBuilder;
import cj.ultimate.util.StringUtil;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

//管理电缆集合
public class ClientGatewaySocket implements IGatewaySocket {
	IServiceProvider parent;
	List<IGatewaySocketCable> cables;// 电览集合,一个uri一个电缆
	private Destination destination;
	private IInputPipelineBuilder inputBuilder;
	private IOutputPipelineBuilder outputBuilder;
	private boolean isConnected;
	EventLoopGroup eventloopGroup;
	public ClientGatewaySocket(IServiceProvider parent) {
		this.parent = parent;
		cables = new ArrayList<>();
		inputBuilder=new ClientSocketInputPipelineBuilder(this);
		outputBuilder=new ClientSocketOutputPipelineBuilder();
	}

	@Override
	public Object getService(String name) {
		if ("$.pipeline.input.builder".equals(name)) {
			return inputBuilder;
		}
		if ("$.pipeline.output.builder".equals(name)) {
			return outputBuilder;
		}
		if ("$.socket".equals(name)) {
			return this;
		}
		if ("$.socket.name".equals(name)) {
			return this.name();
		}
		if("$.destination".equals(name)) {
			return destination;
		}
		if("$.cables".equals(name)) {
			return cables;
		}
		if("$.eventloop.group".equals(name)) {
			return eventloopGroup;
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
		if(isConnected) {
			return;
		}
		this.destination=dest;
		
		String broadcast=dest.getProps().get("Broadcast-Mode");
		if(StringUtil.isEmpty(broadcast)) {
			broadcast="unicast";//multicast
			dest.getProps().put("Broadcast-Mode", broadcast);
		}
		List<String> uris = dest.getUris();
		//控制台配的目标属性是所有电缆共享的，如总广播策略等；而对于目标内的每个uri由于其属性都可能不同，因此按以下连接串的方式配置
		int nThread=0;
		for (String connStr : uris) {//uri表示为connStr： protocol://ip:port?minWireSize=4&maxWireSize=10&xx=...
			IGatewaySocketCable cable = new GatewaySocketCable(this);
			cable.init(connStr);
			nThread+=cable.maxWireSize();
			cables.add(cable);
		}
		nThread+=1;//多给一个线程
		String workThreadCount=dest.getProps().get("workThreadCount");
		if(StringUtil.isEmpty(workThreadCount)) {
			workThreadCount="1";
		}
		int userNThread=Integer.valueOf(workThreadCount);
		if(userNThread<nThread) {
			CJSystem.logging().warn(getClass(), String.format("配置的线程：%s 小于电缆需要的线程数:%s，系统为之分配为：%s",userNThread,nThread,nThread));
		}else {
			nThread=userNThread;
		}
		dest.getProps().put("workThreadCount", nThread+"");
		this.eventloopGroup=new NioEventLoopGroup(nThread);
		for(IGatewaySocketCable cable:cables) {
			cable.connect();
		}
		isConnected=true;
	}

	@Override
	public void close() throws CircuitException {
		for(IGatewaySocketCable cable:cables) {
			cable.close();
		}
		cables.clear();
		parent=null;
		isConnected = false;
		this.inputBuilder = null;
		this.outputBuilder=null;
	}

}
