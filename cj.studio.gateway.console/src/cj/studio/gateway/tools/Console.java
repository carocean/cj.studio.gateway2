package cj.studio.gateway.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceSetter;
import cj.studio.gateway.IGateway;
import cj.ultimate.util.StringUtil;

/**
 * 控制台
 * 
 * <pre>
 * 用于维扩一个循环处理
 * 一个控制台多个命令
 * 控制台分发命令，它就是个命令工厂，命令类处理命令行
 * 有：
 * 	统一存储控制台
	网盘控制台
	立方体控制台
	文件系统控制台
	本地控制台
 * </pre>
 * 
 * @author carocean
 *
 */
public abstract class Console implements IServiceSetter {
	protected Map<String, Command> commands;

	public Console() {
		commands = new HashMap<>();
	}

	@Override
	public void setService(String serviceId, Object service) {

		Command cmd = (Command) service;
		commands.put(cmd.cmd(), cmd);
	}

	protected abstract String prefix(IGateway gateway, Object... target);

	protected abstract boolean exit(String cmd);

	public void monitor(IGateway gateway, Object... target)
			throws IOException {
		InputStreamReader input = new InputStreamReader(System.in);
		BufferedReader read = new BufferedReader(input);
		String prefix = prefix(gateway, target);
		// System.out.println("提示：请用man查看命令");
		System.out.print(prefix);
		String line = "";
		while (true) {
			line = read.readLine();
			if (StringUtil.isEmpty(line)) {
				System.out.print(prefix);
				continue;
			}
			if (exit(line)) {
				Set<String> set = commands.keySet();
				for (String key : set) {
					Command cmd = commands.get(key);
					cmd.dispose();
				}
				break;
			}
			try {// 执行命令工具
				String[] arr = line.split(" ");
				String cmdName = arr[0];
				// String cmdTwo="";
				if ("man".equals(cmdName)) {
					printMan(gateway, target, commands);
					System.out.print(prefix);
					continue;
				}

				String args[] = new String[arr.length - 1];
				if (arr.length > 1)
					// if (arr[1].trim().startsWith("-")) {
					System.arraycopy(arr, 1, args, 0, arr.length - 1);
				// } else {
				//
				// }

				if (!commands.containsKey(cmdName)) {
					throw new EcmException("不认识的命令：" + cmdName);
				}

				Command cmd = commands.get(cmdName);
				GnuParser parser = new GnuParser();
				CommandLine the = parser.parse(cmd.options(), args);
				CmdLine cl = new CmdLine(cmdName, the);
				cl.prop("gateway", gateway);
				cl.prop("prefix", prefix);
				cl.prop("indent", "  ");
				beforDoCommand(cmd, cl);
				cmd.doCommand(cl);
				System.out.print(prefix);
			} catch (Exception e) {
//				e.printStackTrace();
				System.out.println("命令执行错误，原因：\r\n" + e.getMessage());
				if (e instanceof NullPointerException) {
					CJSystem.current().environment().logging().error(getClass(),
							e);
				} else {
					CJSystem.current().environment().logging().error(getClass(),
							e.getMessage());
				}
				System.out.print(prefix);
				continue;
			}
		}

	}

	protected void beforDoCommand(Command cmd, CmdLine cl) {
		// TODO Auto-generated method stub

	}

	protected void printMan(IGateway gateway, Object[] target,
			Map<String, Command> cmds) {
		Set<String> set = cmds.keySet();
		for (String key : set) {
			Command cmd = cmds.get(key);
			HelpFormatter formatter = new HelpFormatter();
			if (cmd.options() != null)
				formatter.printHelp(cmd.cmd(), cmd.cmdDesc(), cmd.options(),
						"----------------");
		}

	}
}
