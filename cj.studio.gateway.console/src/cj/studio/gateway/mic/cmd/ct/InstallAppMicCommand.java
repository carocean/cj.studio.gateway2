package cj.studio.gateway.mic.cmd.ct;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IContentReciever;
import cj.studio.gateway.ICluster;
import cj.studio.gateway.mic.IMicConsoleSession;
import cj.studio.gateway.mic.ISendResponse;
import cj.studio.gateway.mic.MicCommand;
import cj.ultimate.util.StringUtil;

public class InstallAppMicCommand extends MicCommand {

	@Override
	public String cmd() {
		return "site";
	}

	@Override
	public String cmdDesc() {
		return "安装app程序包到应用目标。例：site website";
	}

	@Override
	public Options options() {
		Options options = new Options();
		return options;
	}

	@Override
	public void doCommand(CommandLine line, String user, ISendResponse response, Frame frame,
			IMicConsoleSession session) throws CircuitException {
		StringBuilder sb = new StringBuilder();
		sb.append("<ul>");
		@SuppressWarnings("unchecked")
		List<String> args = line.getArgList();
		if (args.isEmpty()) {
			sb.append("<li><span>缺少目标名</span></li>");
			sb.append("</ul>");
			response.send(user, sb.toString());
			return;
		}
		String domain = args.get(0);
		if (StringUtil.isEmpty(domain)) {
			sb.append("<li><span>缺少目标名</span></li>");
			sb.append("</ul>");
			response.send(user, sb.toString());
			return;
		}
		ICluster cluster = (ICluster) session.provider().getService("$.cluster");
		if(!cluster.containsValid(domain)) {
			sb.append(String.format("<li><span>目标:%s 不存在</span></li>",domain));
			sb.append("</ul>");
			response.send(user, sb.toString());
			return;
		}
		String fileName = frame.parameter("fileName");
		String gatewayHome = (String) session.provider().getService("$.homeDir");
		String dir = String.format("%s%sassemblies%s%s%s", gatewayHome, File.separator, File.separator, domain,
				File.separator);
		File dirF=new File(dir);
		if(!dirF.exists()) {
			dirF.mkdirs();
		}
		String file=String.format("%s%s", dir,fileName);
		frame.content().accept(new FileContentReciever(file, sb,user,response));
	}

	class FileContentReciever implements IContentReciever {
		private FileOutputStream out;
		String file;
		StringBuilder sb;
		ISendResponse response;
		String user;
		public FileContentReciever(String file, StringBuilder sb,String user,ISendResponse response) {
			this.file = file;
			this.sb = sb;
			this.response=response;
			this.user=user;
		}

		@Override
		public void recieve(byte[] b, int pos, int length) throws CircuitException {
			try {
				out.write(b, pos, length);
			} catch (IOException e) {
				sb.append(String.format("<li>%s</li>", e));
				sb.append("</ul>");
				response.send(user, sb.toString());
				throw new EcmException(e);
			}
		}

		@Override
		public void done(byte[] b, int pos, int length) throws CircuitException {
			try {
				out.write(b, pos, length);
				out.close();
				sb.append("<li>文件写入成功</li>");
				sb.append("<li>完成命令</li>");
				sb.append("</ul>");
				response.send(user, sb.toString());
			} catch (IOException e) {
				sb.append(String.format("<li>%s</li>", e));
				sb.append("</ul>");
				response.send(user, sb.toString());
				throw new EcmException(e);
			}

		}

		@Override
		public void begin(Frame frame) {
			try {
				out = new FileOutputStream(file);
				sb.append(String.format("<li>开始写入文件：%s</li>", new File(file).getName()));
			} catch (FileNotFoundException e) {
				sb.append(String.format("<li>%s</li>", e));
				sb.append("</ul>");
				try {
					response.send(user, sb.toString());
				} catch (CircuitException e1) {
				}
				throw new EcmException(e);
			}

		}
	}
}
