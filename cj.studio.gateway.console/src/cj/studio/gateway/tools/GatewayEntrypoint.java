package cj.studio.gateway.tools;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.log4j.Logger;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.gateway.IGateway;

@CjService(name = "gatewayEntrypoint", isExoteric = true)
public class GatewayEntrypoint {
	@CjServiceRef(refByName = "gateway")
	private IGateway gateway;
	@CjServiceRef(refByName = "routerConsole")
	private RouterConsole console;
	Logger logger = Logger.getLogger(GatewayEntrypoint.class);
	public void setHomeDir(String homeDir){
		gateway.setHomeDir(homeDir);
	}
	public void main(CommandLine line) {
		gateway.start();
		if(line.hasOption("nohup")) {
			logger.info("网关以nohup方式成功启动");
			return;
		}
		logger.info("网关成功启动");
		try {
			console.monitor(gateway);
		} catch (IOException e1) {
			logger.info("网关监视失败。原因："+e1.getMessage());
		}
		System.out.println("正在退出...");
		gateway.stop();
		try {// 如果3秒后还没退出，则强制
			Thread.sleep(3000);
		} catch (InterruptedException e) {

		} finally {
			System.exit(0);
		}
	}

}
