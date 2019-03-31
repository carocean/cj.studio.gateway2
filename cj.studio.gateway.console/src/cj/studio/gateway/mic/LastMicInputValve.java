package cj.studio.gateway.mic;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.socket.pipeline.IIPipeline;
import cj.studio.gateway.socket.pipeline.IInputValve;

public class LastMicInputValve implements IInputValve {
	IMicCommandFactory factory;
	public LastMicInputValve(IServiceProvider parent) {
		factory=new MicCommandFactory(parent);
	}
	@Override
	public void onActive(String inputName, IIPipeline pipeline) throws CircuitException {
		pipeline.nextOnActive(inputName, this);
	}

	@Override
	public void flow(Object request, Object response, IIPipeline pipeline) throws CircuitException {
		Frame frame = (Frame) request;
		if ("notify".equals(frame.command())) {
			String relativePath = frame.relativePath();
			while (relativePath.endsWith("/")) {
				relativePath = relativePath.substring(0, relativePath.length() - 1);
			}
			if (relativePath.startsWith("/error/")) {
				int pos = relativePath.lastIndexOf("/");
				String pageName = "";
				if (pos > -1) {
					pageName = relativePath.substring(pos + 1, relativePath.length());
				} else {
					pageName = relativePath;
				}
				if ("register-location-error.service".equals(pageName)) {
					CJSystem.logging().error(getClass(), String.format("注册失败。微服务中心不存在注册路径：%s", frame.parameter("location")));
					return;
				}
			}
			return;
		}
		if("exe".equals(frame.command())) {
			String cmdline=frame.parameter("cmdline");
			String user=frame.parameter("user");
			factory.exeCommand(cmdline,user);
			return;
		}
		pipeline.nextFlow(request, response, this);
	}

	@Override
	public void onInactive(String inputName, IIPipeline pipeline) throws CircuitException {
		pipeline.nextOnInactive(inputName, this);
	}

}
