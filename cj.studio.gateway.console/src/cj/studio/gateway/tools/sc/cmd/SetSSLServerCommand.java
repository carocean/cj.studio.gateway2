package cj.studio.gateway.tools.sc.cmd;

import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceInvertInjection;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.gateway.IConfiguration;
import cj.studio.gateway.IGateway;
import cj.studio.gateway.IGatewayServer;
import cj.studio.gateway.IGatewayServerContainer;
import cj.studio.gateway.conf.ServerInfo;
import cj.studio.gateway.server.HttpGatewayServer;
import cj.studio.gateway.server.secure.ISSLEngineFactory;
import cj.studio.gateway.tools.CmdLine;
import cj.studio.gateway.tools.Command;
import cj.studio.gateway.tools.Console;
import cj.ultimate.util.StringUtil;

@CjService(name = "setSSLServerCommand")
public class SetSSLServerCommand extends Command {
	@CjServiceInvertInjection
	@CjServiceRef(refByName = "serversConsole")
	Console console;

	public void doCommand(CmdLine cl) throws IOException {
		IGateway gateway = (IGateway) cl.prop("gateway");
		CommandLine line = cl.line();
		@SuppressWarnings("unchecked")
		List<String> args = line.getArgList();
		String indent = (String) cl.prop("indent");
		if (args.isEmpty()) {
			System.out.println(String.format("%s错误：未指定服务器名", indent));
			return;
		}
		String name = args.get(0);
		IGatewayServerContainer container = (IGatewayServerContainer) gateway.getService("$.container.server");
		if (!container.containsServer(name)) {
			System.out.println(String.format("%s服务器不存在：%s。", indent, name));
			return;
		}
		IGatewayServer server = container.server(name);
		if(!(server instanceof HttpGatewayServer)) {
			System.out.println(String.format("仅支持http server以实现https方式访问。", indent));
			return;
		}
		ServerInfo info = (ServerInfo) server.getService("$.server.info");
		if (!line.hasOption("f")) {
			System.out.println(String.format("%s错误：缺少参数f。", indent));
			return;
		}
		info.getProps().put("KeyStore-File", line.getOptionValue("f"));
		String t = line.getOptionValue("t");
		if (StringUtil.isEmpty(t)) {
			t = "JKS";
		}
		info.getProps().put("KeyStore-Type", t);
		if (!line.hasOption("kp")) {
			System.out.println(String.format("%s错误：缺少参数kp。", indent));
			return;
		}
		info.getProps().put("KeyStore-Password", line.getOptionValue("kp"));
		if (!line.hasOption("cp")) {
			System.out.println(String.format("%s错误：缺少参数cp。", indent));
			return;
		}
		info.getProps().put("Cert-Password", line.getOptionValue("cp"));

		info.getProps().put("SSL-Enabled", "true");

		ISSLEngineFactory factory = (ISSLEngineFactory) server.getService("$.server.sslEngine");
		factory.refresh();

		IConfiguration conf = (IConfiguration) gateway.getService("$.config");
		conf.flushServers();
	}

	@Override
	public String cmd() {
		return "ssl";
	}

	@Override
	public String cmdDesc() {
		return "开启https，格式：ssl httpSite -f ~/keystore/https.keystore -t JKS -kp ks1234 -cp my1234";
	}

	@Override
	public Options options() {
		Options options = new Options();
		Option c = new Option("f", "keystoreFile", true,
				"设置keystore文件的路径，如果放在网关内可使用相对符前缀：~/，如：~/keystore/https.keystore");
		options.addOption(c);
		Option t = new Option("t", "keystoreType", false, "keystore类型。如JKS,jceks,dks,pkcs11,pkcs12");
		options.addOption(t);
		Option r = new Option("kp", "keyStorePassword", true, "keystore的登录密码");
		options.addOption(r);
		Option cp = new Option("cp", "certPassword", true, "keystore中证书的密码");
		options.addOption(cp);
		return options;
	}
}
