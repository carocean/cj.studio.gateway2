package cj.studio.gateway;

public interface IMicNode {
	static String SERVICE_KEY="$.mic.node.current";
	String guid();
	String title();
	String desc();
	boolean isEnabled();
	IMicNodeInfo info();
}
