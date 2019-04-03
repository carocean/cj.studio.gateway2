package cj.studio.gateway;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceInvertInjection;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.logging.ILogging;
import cj.studio.gateway.conf.Configuration;
import cj.studio.gateway.server.util.DefaultHttpMineTypeFactory;

@CjService(name = "gateway")
public class Gateway implements IGateway, IServiceProvider {
	private IConfiguration config;
	ILogging logger;
	ISupportProtocol supportProtocol;
	@CjServiceInvertInjection
	@CjServiceRef(refByName = "gatewayServerContainer")
	private IGatewayServerContainer servercontainer;

	@CjServiceInvertInjection
	@CjServiceRef(refByName = "gatewaySocketContainer")
	private IGatewaySocketContainer socketContainer;

	@CjServiceRef(refByName = "junctionTable")
	private IJunctionTable junctionTable;

	@CjServiceInvertInjection
	@CjServiceRef(refByName = "dloader")
	IDestinationLoader dloader;

	@CjServiceInvertInjection
	@CjServiceRef(refByName = "micConnector")
	IMicConnector micConnector;

	public Gateway() {
		logger = CJSystem.logging();
	}

	@Override
	public Object getService(String name) {
		if ("$.container.server".equals(name)) {
			return servercontainer;
		}
		if ("$.container.socket".equals(name)) {
			return socketContainer;
		}

		if ("$.dloader".equals(name)) {
			return dloader;
		}
		if ("$.config".equals(name)) {
			return config;
		}
		if ("$.cluster".equals(name)) {
			return config.getCluster();
		}
		if ("$.junctions".equals(name)) {
			return this.junctionTable;
		}
		if ("$.homeDir".equals(name)) {
			return String.format("%s", config.home());
		}
		if ("$.supportProtocol".equals(name)) {
			return supportProtocol;
		}
		if (IMicNode.SERVICE_KEY.equals(name)) {
			if (micConnector == null)
				return null;
			IServiceProvider provider = (IServiceProvider) micConnector;
			IMicNode node = (IMicNode) provider.getService(name);
			return node;
		}
		return null;
	}

	@Override
	public <T> ServiceCollection<T> getServices(Class<T> clazz) {

		return null;
	}

	@Override
	public void start() {
		supportProtocol = new SupportProtocol();
		config.load();
		servercontainer.startAll();
		if (config.registry().isEnabled()) {
			micConnector.init();
			micConnector.connect();
		}
		logger.info("-------------网关启动完毕-------------------");
	}

	@Override
	public void stop() {
		// 停止所有服务程序
		servercontainer.stopAll();
		if (config.registry().isEnabled()) {
			micConnector.disconnect();
		}
	}

	@Override
	public void setHomeDir(String homeDir) {
		config = new Configuration(homeDir);
		DefaultHttpMineTypeFactory.setHomeDir(homeDir);
	}

	@Override
	public boolean supportProtocol(String protocol) {
		return this.supportProtocol.supportProtocol(protocol);
	}

	class SupportProtocol implements ISupportProtocol {

		@Override
		public boolean supportProtocol(String protocol) {
			return "ws".equals(protocol) || "http".equals(protocol) || "tcp".equals(protocol) || "udt".equals(protocol)
					|| "jms".equals(protocol) || "app".equals(protocol);
		}

	}
}
