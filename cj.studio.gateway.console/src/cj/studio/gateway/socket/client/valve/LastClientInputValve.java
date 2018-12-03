package cj.studio.gateway.socket.client.valve;

import java.util.ArrayList;
import java.util.List;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.graph.CircuitException;
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
	int trytimes;// 重试次数

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
		trytimes = 0;
		select(pipeline);
	}

	@Override
	public void flow(Object request, Object response, IIPipeline pipeline) throws CircuitException {
		// 如果发现写失败则重新选择导线
		// 由于pipeline在同一线程下，所以不必考虑选择或释放导线的并发问题，因为不存在并发
		if (trytimes >= 3) {
			CJSystem.logging().error(getClass(), "电缆无可用导线，已尝试3次，请求被丢弃：" + request);
			return;
		}
		if (wires.isEmpty()) {
			select(pipeline);
			return;
		}
		IGatewaySocketWire[] arr = wires.toArray(new IGatewaySocketWire[0]);
		for (IGatewaySocketWire w : arr) {
			if (w == null) {
				continue;
			}
			if (!w.isWritable() && !w.isOpened()) {
				select(pipeline);// 重新选择所有导线
				break;
			}
			try {
				w.send(request, response);
				trytimes = 0;
			} catch (Throwable e) {
				trytimes++;// 重新再来n次，直到可用，select方法会堵塞直到拥有可使用导线
				CJSystem.logging().error(getClass(), e + "");
				flow(request, response, pipeline);
				break;
			}
		}
	}

	@Override
	public void onInactive(String inputName, IIPipeline pipeline) throws CircuitException {
		// 归还导线到电缆
		returnWire();
		trytimes = 0;
	}

	protected void returnWire() {
		IGatewaySocketWire[] arr = wires.toArray(new IGatewaySocketWire[0]);
		for (IGatewaySocketWire w : arr) {
			if (w != null) {
				w.used(false);
			}
		}
		wires.clear();
	}

	protected synchronized void select(IIPipeline pipeline) throws CircuitException {
		returnWire();// 释放导线重新选择
		String boardcast = destination.getProps().get("Broadcast-Mode");
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
			if (wire != null) {
				wires.add(wire);
			}
		}
	}
}
