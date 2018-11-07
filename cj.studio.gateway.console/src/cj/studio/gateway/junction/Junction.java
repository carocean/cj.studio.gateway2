package cj.studio.gateway.junction;

public abstract class Junction {
	String name;
	long createTime;
	public Junction(String name) {
		this.name=name;
		this.createTime=System.currentTimeMillis();
	}
	public String getName() {
		return name;
	}
	public long getCreateTime() {
		return createTime;
	}
}
