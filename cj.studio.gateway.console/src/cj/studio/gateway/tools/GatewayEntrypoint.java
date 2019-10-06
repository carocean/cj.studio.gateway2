package cj.studio.gateway.tools;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.logging.ILogging;
import cj.studio.gateway.IGateway;
import org.apache.commons.cli.CommandLine;

import java.io.IOException;

@CjService(name = "gatewayEntrypoint", isExoteric = true)
public class GatewayEntrypoint {
	@CjServiceRef(refByName = "gateway")
	private IGateway gateway;
	@CjServiceRef(refByName = "routerConsole")
	private RouterConsole console;
	ILogging logger ;
	public void setHomeDir(String homeDir){
		gateway.setHomeDir(homeDir);
		logger= CJSystem.logging();
	}
	public void main(CommandLine line) {
		gateway.start();
		if(line.hasOption("nohup")) {
			logger.info(getClass(),"网关以nohup方式成功启动");
			return;
		}
		logger.info(getClass(),"网关成功启动");
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
