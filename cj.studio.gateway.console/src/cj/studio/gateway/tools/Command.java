package cj.studio.gateway.tools;

import java.io.IOException;

import org.apache.commons.cli.Options;

/**
 * 用于定义和执行命令行
 * <pre>
 *
 * </pre>
 * @author carocean
 *
 */
public abstract class Command {
	public abstract String cmd();
	public abstract String cmdDesc();
	public abstract Options options();
	public abstract void doCommand(CmdLine cl) throws IOException;
	protected void dispose(){
		
	}
}
