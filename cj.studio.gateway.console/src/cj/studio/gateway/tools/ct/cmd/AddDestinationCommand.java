package cj.studio.gateway.tools.ct.cmd;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceInvertInjection;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.gateway.ICluster;
import cj.studio.gateway.IConfiguration;
import cj.studio.gateway.IGateway;
import cj.studio.gateway.socket.Destination;
import cj.studio.gateway.tools.CmdLine;
import cj.studio.gateway.tools.Command;
import cj.studio.gateway.tools.Console;
import cj.ultimate.util.StringUtil;

@CjService(name = "addDestinationCommand")
public class AddDestinationCommand extends Command {
	@CjServiceInvertInjection
	@CjServiceRef(refByName = "clusterConsole")
	Console console;
	

	public void doCommand(CmdLine cl) throws IOException {
		IGateway gateway = (IGateway) cl.prop("gateway");
		CommandLine line = cl.line();
		@SuppressWarnings("unchecked")
		List<String> args = line.getArgList();
		String indent = (String) cl.prop("indent");
		if (args.isEmpty()) {
			System.out.println(String.format("%s错误：未指定逻辑域名", indent));
			return;
		}
		String domain = args.get(0);
		Destination item=new Destination(domain);
		
		if(!line.hasOption("h")){
			System.out.println(String.format("%s错误：缺少参数h。", indent));
			return;
		}
		if(line.hasOption("ar")) {
			item.getProps().put("autorun", "true");
		}
		String host=line.getOptionValue("h");
		String[] harr=host.split(",");
		
		for(String h:harr){
			if(StringUtil.isEmpty(h))continue;
			item.getUris().add(h);
		}
		Map<String, String> props=item.getProps();
		if (line.hasOption("P")) {
			Properties list = line.getOptionProperties("P");
			for (Object k : list.keySet()) {
				props.put((String) k, list.getProperty((String) k));
			}
		}
		
		ICluster cluster =(ICluster)gateway.getService("$.cluster");
		cluster.addDestination(item);
		IConfiguration config =(IConfiguration)gateway.getService("$.config");
		config.flushCluster();
	}

	@Override
	public String cmd() {
		return "add";
	}

	@Override
	public String cmdDesc() {
		return "添加群族的远程目标，格式：add domain -h xxx 例：add yanxin -h ip:port,ip:port。\r\n"
				+ "注意：域名在同一网关中唯一。"
				+ "\r\n支持的协议有：tcp,udt,http,ws,jms,app\r\n"
				+ "如果是app协议，则：uri格式是：app://程序相对目录名:适配类型,例：app://wigo:way,适配类型有：jee,way";
	}

	@SuppressWarnings("static-access")
	@Override
	public Options options() {
		Options options = new Options();
		Option c = new Option("h", "hosts", true, "主机地址（ip:port)，如果有多个则以,号分隔");
		options.addOption(c);
		Option at = new Option("ar", "autorun", false, "自动运行，在网关启动时");
		options.addOption(at);
		Option p = OptionBuilder.withArgName("property=value").hasArgs(2)
				.withValueSeparator()
				.withDescription("设置服务器相关属性,格式为：-Pproperty=value 如心跳间隔：-PheartbeatInterval=1000,-PwaitMax=毫秒（表示连接池满后等待可用的超时时间）,-PmaxTotal=最大连接数").create("P");
		options.addOption(p);
		return options;
	}
}
