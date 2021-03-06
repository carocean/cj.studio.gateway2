package cj.studio.gateway.socket.pipeline;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class OutputPipelineCollection {
	Map<String, IOutputPipeline> map;
	public OutputPipelineCollection() {
		map=new ConcurrentHashMap<>();
	}
	public Set<String> enumName(){
		return map.keySet();
	}
	public boolean containsName(String name) {
		return map.containsKey(name);
	}
	public IOutputPipeline get(String name) {
		return map.get(name);
	}
	public void add(String name,IOutputPipeline pipeline) {
		map.put(name, pipeline);
	}
	public void remove(String name) {
		map.remove(name);
	}
	public boolean isEmpty() {
		return map.isEmpty();
	}
}
