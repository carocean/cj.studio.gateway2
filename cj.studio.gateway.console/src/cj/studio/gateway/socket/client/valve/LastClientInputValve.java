package cj.studio.gateway.socket.client.valve;

import java.util.List;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.socket.Destination;
import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.cable.IGatewaySocketCable;
import cj.studio.gateway.socket.cable.IGatewaySocketWire;
import cj.studio.gateway.socket.pipeline.IIPipeline;
import cj.studio.gateway.socket.pipeline.IInputValve;
import cj.studio.gateway.socket.pipeline.IValveDisposable;
import cj.studio.gateway.socket.util.HashFunction;
import cj.studio.gateway.socket.util.SocketContants;

public class LastClientInputValve implements IInputValve, IValveDisposable {
	private Destination destination;
	private List<IGatewaySocketCable> cables;
	HashFunction hash;
	private IGatewaySocket socket;

	@SuppressWarnings("unchecked")
	public LastClientInputValve(IServiceProvider parent) {
		socket = (IGatewaySocket) parent.getService("$.socket");
		destination = (Destination) parent.getService("$.destination");
		cables = (List<IGatewaySocketCable>) parent.getService("$.cables");
		hash = new HashFunction();
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
		IGatewaySocketWire seleted = select(pipeline);
		if (seleted == null) {
			throw new CircuitException("404", "未选择到可用导线");
		}
		if (!seleted.isOpened()) {
			throw new CircuitException("404", "导线未打开");
		}
		try {
			seleted.send(request, response);
		} catch (Throwable e) {
			CJSystem.logging().error(getClass(), e + "");
		}
	}

	@Override
	public void onInactive(String inputName, IIPipeline pipeline) throws CircuitException {
		// 归还导线到电缆
		for (IGatewaySocketCable cable : cables) {
			cable.unselect();
		}
	}

	protected IGatewaySocketWire select(IIPipeline pipeline) throws CircuitException {
		if (cables.size() < 0) {
			return null;
		}
		String boardcast = destination.getProps().get("broadcast");
		String name = pipeline.prop(SocketContants.__pipeline_name);
		if ("unicast".equals(boardcast)) {// 单播是均衡的选一个电缆
			long v = hash.hash(name);
			int index = (int) (Math.abs(v) % cables.size());
			IGatewaySocketCable cable = cables.get(index);
			IGatewaySocketWire wire = cable.select();
			if (wire != null && wire.isOpened()) {
				return wire;
			}
		}

		// 多播是每个电览选一个导线
		for (IGatewaySocketCable cable : cables) {
			IGatewaySocketWire wire = cable.select();
			if (wire == null) {
				continue;
			}
			if (!wire.isOpened()) {
				continue;
			}
			return wire;
		}
		return null;
	}

	@Override
	public void dispose(boolean isCloseableOutputValve) {
		if (isCloseableOutputValve) {
			try {
				socket.close();
			} catch (CircuitException e) {
				throw new EcmException(e);
			}
		}
		this.cables = null;
		this.destination = null;
		this.socket = null;
	}
}
