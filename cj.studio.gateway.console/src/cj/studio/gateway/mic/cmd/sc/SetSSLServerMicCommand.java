package cj.studio.gateway.mic.cmd.sc;

import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.IConfiguration;
import cj.studio.gateway.IGatewayServer;
import cj.studio.gateway.IGatewayServerContainer;
import cj.studio.gateway.conf.ServerInfo;
import cj.studio.gateway.mic.IMicConsoleSession;
import cj.studio.gateway.mic.ISendResponse;
import cj.studio.gateway.mic.MicCommand;
import cj.studio.gateway.server.HttpGatewayServer;
import cj.studio.gateway.server.secure.ISSLEngineFactory;
import cj.ultimate.util.StringUtil;

public class SetSSLServerMicCommand extends MicCommand {

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

	@Override
	public void doCommand(CommandLine line, String user, ISendResponse response, IMicConsoleSession session)
			throws CircuitException {
		@SuppressWarnings("unchecked")
		List<String> args = line.getArgList();
		String indent = "&nbsp;";
		StringBuilder sb=new StringBuilder();
		sb.append("<ul>");
		if (args.isEmpty()) {
			sb.append(String.format("<li style='padding-left:5px;'>%s错误：未指定服务器名</li>", indent));
			sb.append("</ul>");
			response.send(user, sb.toString());
			return;
		}
		String name = args.get(0);
		IGatewayServerContainer container = (IGatewayServerContainer) session.provider().getService("$.container.server");
		if (!container.containsServer(name)) {
			sb.append(String.format("<li style='padding-left:5px;'>%s服务器不存在：%s。</li>", indent, name));
			sb.append("</ul>");
			response.send(user, sb.toString());
			return;
		}
		IGatewayServer server = container.server(name);
		if(!(server instanceof HttpGatewayServer)) {
			sb.append(String.format("<li style='padding-left:5px;'>仅支持http server以实现https方式访问。</li>", indent));
			sb.append("</ul>");
			response.send(user, sb.toString());
			return;
		}
		ServerInfo info = (ServerInfo) server.getService("$.server.info");
		if (!line.hasOption("f")) {
			sb.append(String.format("<li style='padding-left:5px;'>%s错误：缺少参数f。</li>", indent));
			sb.append("</ul>");
			response.send(user, sb.toString());
			return;
		}
		info.getProps().put("KeyStore-File", line.getOptionValue("f"));
		String t = line.getOptionValue("t");
		if (StringUtil.isEmpty(t)) {
			t = "JKS";
		}
		info.getProps().put("KeyStore-Type", t);
		if (!line.hasOption("kp")) {
			sb.append(String.format("<li style='padding-left:5px;'>%s错误：缺少参数kp。</li>", indent));
			sb.append("</ul>");
			response.send(user, sb.toString());
			return;
		}
		info.getProps().put("KeyStore-Password", line.getOptionValue("kp"));
		if (!line.hasOption("cp")) {
			sb.append(String.format("<li style='padding-left:5px;'>%s错误：缺少参数cp。</li>", indent));
			sb.append("</ul>");
			response.send(user, sb.toString());
			return;
		}
		info.getProps().put("Cert-Password", line.getOptionValue("cp"));

		info.getProps().put("SSL-Enabled", "true");

		ISSLEngineFactory factory = (ISSLEngineFactory) server.getService("$.server.sslEngine");
		factory.refresh();

		IConfiguration conf = (IConfiguration) session.provider().getService("$.config");
		conf.flushServers();
	}

}
