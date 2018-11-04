package cj.studio.gateway.tools;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;


public final class CmdLine {
	String cmd;
	CommandLine line;
	Map<String,Object> props;
	public CmdLine(String cmd, CommandLine line) {
		this.cmd=cmd;
		this.line=line;
		props=new HashMap<>();
	}
	public String cmd() {
		return cmd;
	}
	public CommandLine line() {
		return line;
	}
	public String propString(String key){
		return (String)props.get(key);
	}
	public Object prop(String key){
		return props.get(key);
	}
	public void prop(String key,Object v){
		props.put(key, v);
	}
	public void copyPropsFrom(CmdLine cl) {
		props.putAll(cl.props);
	}
}
