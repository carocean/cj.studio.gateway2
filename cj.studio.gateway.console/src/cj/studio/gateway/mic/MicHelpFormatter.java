package cj.studio.gateway.mic;

import java.io.PrintWriter;
import java.util.Collection;

import org.apache.commons.cli.Option;

public class MicHelpFormatter implements IHelpFormatter {

	@Override
	public void printHelp(PrintWriter pw, MicCommand cmd) {
		pw.append(String.format("<li><span>%s</span></li>",cmd.cmd()));
		pw.append(String.format("<li style='padding-left:5px;'><span>%s</span></li>",cmd.cmdDesc()));
		@SuppressWarnings("unchecked")
		Collection<Option> col=cmd.options().getOptions();
		for(Option op:col) {
			String text=String.format("<li style='padding-left:20px;'><span>&nbsp;&nbsp;-%s%s,%s&nbsp;&nbsp;%s</span></li>",op.getOpt(),op.getLongOpt()==null?(",&lt;"+op.getArgName()+"&gt;"):(",--"+op.getLongOpt()),op.hasArg()?"必选":"可选",op.getDescription());
			pw.append(text);
		}
	}

}
