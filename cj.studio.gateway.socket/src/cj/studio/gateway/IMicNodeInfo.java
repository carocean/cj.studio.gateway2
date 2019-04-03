package cj.studio.gateway;

public interface IMicNodeInfo {
	String location();
	String host();
	long reconnDelay();
	long reconnPeriod();
	String cjtoken();
}
