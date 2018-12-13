package cj.studio.gateway.socket.client.valve;

import java.util.ArrayList;
import java.util.List;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.socket.Destination;
import cj.studio.gateway.socket.cable.IGatewaySocketCable;
import cj.studio.gateway.socket.cable.IGatewaySocketWire;
import cj.studio.gateway.socket.pipeline.IIPipeline;
import cj.studio.gateway.socket.pipeline.IInputValve;
import cj.studio.gateway.socket.util.HashFunction;
import cj.studio.gateway.socket.util.SocketContants;

public class LastClientInputValve implements IInputValve {
	private Destination destination;
	List<IGatewaySocketWire> wires;
	private List<IGatewaySocketCable> cables;
	HashFunction hash;

	@SuppressWarnings("unchecked")
	public LastClientInputValve(IServiceProvider parent) {
		destination = (Destination) parent.getService("$.destination");
		cables = (List<IGatewaySocketCable>) parent.getService("$.cables");
		hash = new HashFunction();
		wires = new ArrayList<>();
	}

	@Override
	public void onActive(String inputName, IIPipeline pipeline) throws CircuitException {
		// 选择导线,留给app端开发者关闭输出管道时关闭，此处不考虑关闭。
		// 也就是导线的释放权交由app端输出管道来控制。
		select(pipeline);
	}

	@Override
	public void flow(Object request, Object response, IIPipeline pipeline) throws CircuitException {
		// 如果发现写失败则重新选择导线
		// 由于pipeline在同一线程下，所以不必考虑选择或释放导线的并发问题，因为不存在并发
		if (wires.isEmpty()) {
			select(pipeline);
			return;
		}
		for (IGatewaySocketWire w : wires) {
			if (w == null) {
				continue;
			}
			if (!w.isWritable() || !w.isOpened()) {
				select(pipeline);// 重新选择所有导线
			}
			try {
				w.send(request, response);
			} catch (Throwable e) {
				CJSystem.logging().error(getClass(), e + "");
				continue;
			}
		}
	}

	@Override
	public void onInactive(String inputName, IIPipeline pipeline) throws CircuitException {
		// 归还导线到电缆
		returnWire();
	}

	protected void returnWire() {
		for (IGatewaySocketWire w : wires) {
			if (w != null) {
				w.used(false);
			}
		}
		wires.clear();
	}

	protected void select(IIPipeline pipeline) throws CircuitException {
		if(cables.size()<0) {
			return;
		}
		returnWire();// 释放导线重新选择
		String boardcast = destination.getProps().get("broadcast");
		String name = pipeline.prop(SocketContants.__pipeline_name);
		if ("unicast".equals(boardcast)) {// 单播是均衡的选一个电缆
			long v = hash.hash(name);
			int index = (int) (Math.abs(v) % cables.size());
			IGatewaySocketCable cable = cables.get(index);
			IGatewaySocketWire wire = cable.select();
			if (wire != null) {
				wires.add(wire);
			}
			return;
		}

		// 多播是每个电览选一个导线
		for (IGatewaySocketCable cable : cables) {
			IGatewaySocketWire wire = cable.select();
			if (wire == null) {
				continue;
			}
			if(!wire.isOpened()) {
				continue;
			}
			wires.add(wire);
		}
	}
}
