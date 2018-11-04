package cj.studio.gateway;

public abstract class Junction {
	String name;
	public Junction(String name) {
		this.name=name;
	}
	public String getName() {
		return name;
	}
}
