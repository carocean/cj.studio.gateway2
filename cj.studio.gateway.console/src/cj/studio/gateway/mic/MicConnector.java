package cj.studio.gateway.mic;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.IServiceSetter;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IInputChannel;
import cj.studio.ecm.net.IOutputChannel;
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.ecm.net.io.MemoryInputChannel;
import cj.studio.ecm.net.io.MemoryOutputChannel;
import cj.studio.gateway.IConfiguration;
import cj.studio.gateway.IGatewaySocketContainer;
import cj.studio.gateway.IMicConnector;
import cj.studio.gateway.socket.Destination;
import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.cable.IGatewaySocketCable;
import cj.studio.gateway.socket.client.ClientGatewaySocket;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import cj.studio.gateway.socket.pipeline.IInputPipelineBuilder;

@CjService(name = "micConnector")
public class MicConnector extends TimerTask implements IMicConnector, IServiceSetter ,IServiceProvider{
	IServiceProvider parent;
	IGatewaySocket micSenderSocket;
	IGatewaySocket micReceiverSocket;// 用于接收
	IGatewaySocketContainer sockets;
	Timer timer;// 用于监控连接当断开时重连
	MicRegistry registry;
	private IInputPipeline micSenderInput;
	static String micclient = "mic@gateway.client";
	static String micsender="4082E611-C880-46DC-AB93-BAA975654803@mic";
	@Override
	public void setService(String serviceId, Object service) {
		parent = (IServiceProvider) service;
	}
	@Override
	public Object getService(String serviceId) {
		if("$.sender.input".equals(serviceId)) {
			return micSenderInput;
		}
		return parent.getService(serviceId);
	}
	@Override
	public <T> ServiceCollection<T> getServices(Class<T> serviceClazz) {
		return parent.getServices(serviceClazz);
	}
	@Override
	public void run() {
		@SuppressWarnings("unchecked")
		List<IGatewaySocketCable> cables = (List<IGatewaySocketCable>) micSenderSocket.getService("$.cables");// $.wires.count
		boolean reconnected = true;
		for (IGatewaySocketCable cable : cables) {
			IServiceProvider p = (IServiceProvider) cable;
			int count = (int) p.getService("$.wires.count");
			if (count > 0) {
				reconnected = false;
				break;
			}
		}
		if (reconnected) {
			CJSystem.logging().info(getClass(), "正在重连mic...");
			disconnect(false);
			connect();
		}
	}

	@Override
	public void init() {
		IConfiguration config = (IConfiguration) parent.getService("$.config");
		registry = config.registry();
		timer = new Timer(true);
		timer.schedule(this, registry.getMic().getReconnDelay(), registry.getMic().getReconnPeriod());
	}

	@Override
	public void connect() {
		sockets = (IGatewaySocketContainer) parent.getService("$.container.socket");
		try {
			micReceiverSocket = new MicGatewaySocket(this);
			Destination micReceiverDest = new Destination(micclient);
//			micReceiverDest.getUris().add("app://localhost:mic");
			micReceiverSocket.connect(micReceiverDest);
			sockets.add(micReceiverSocket);

			micSenderSocket = new ClientGatewaySocket(this);
			Destination micSenderDest = new Destination(micsender);
			String host = "";
			if (registry.getMic().getHost().indexOf("OnChannelEvent-Notify-Dests") < 0) {
				if (registry.getMic().getHost().contains("?")) {
					host = String.format("%s&OnChannelEvent-Notify-Dests=%s", registry.getMic().getHost(), micclient);
				} else {
					host = String.format("%s?OnChannelEvent-Notify-Dests=%s", registry.getMic().getHost(), micclient);
				}
			} else {
				host = registry.getMic().getHost();
			}
			micSenderDest.getUris().add(host);
			micSenderDest.getProps().put("location", registry.getMic().getLocation());
			micSenderSocket.connect(micSenderDest);

			IInputPipelineBuilder micSenderBuilder = (IInputPipelineBuilder) micSenderSocket
					.getService("$.pipeline.input.builder");
			micSenderInput = micSenderBuilder.createPipeline();
			sockets.add(micSenderSocket);

			sendRegistry();

			CJSystem.logging().info(getClass(), String.format("注册mic成功。host %s,location %s",
					registry.getMic().getHost(), registry.getMic().getLocation()));
		} catch (Exception e) {
			sockets.remove(micclient);
			sockets.remove(micsender);
			CJSystem.logging().error(getClass(), "注册mic失败，原因：" + e);
			return;
		}
	}

	protected void sendRegistry() throws CircuitException {
		IInputChannel in = new MemoryInputChannel();
		Frame f = new Frame(in, "register /mic/node.service mic/1.0");
		f.parameter("uuid", registry.getGuid());
		f.parameter("title", registry.getTitle());
		f.parameter("desc", registry.getDesc());
		f.parameter("cjtoken",registry.getMic().getCjtoken());
		f.parameter("location", registry.getMic().getLocation());
		f.parameter("micient",micclient);
		f.content().accept(new MemoryContentReciever());
		in.begin(f);
		in.done(new byte[0], 0, 0);
		
		IOutputChannel output=new MemoryOutputChannel();
		Circuit c=new Circuit(output, "mic/1.0 200 OK");
		micSenderInput.headFlow(f, c);
	}

	public void disconnect(boolean disposing) {
		if (disposing) {
			timer.cancel();
		}
		sockets.remove(micclient);
		sockets.remove(micsender);
		try {
			if (micSenderSocket != null) {
				micSenderSocket.close();
			}
			if (micReceiverSocket != null) {
				micReceiverSocket.close();
			}
		} catch (CircuitException e) {
			throw new EcmException(e);
		}
		if (disposing) {
			registry = null;
			CJSystem.logging().error(getClass(), "断开了与mic的连接");
		}
	}

	@Override
	public void disconnect() {
		disconnect(true);
	}

}
