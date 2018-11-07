package cj.studio.gateway.socket.pipeline;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class OutputPipelineCollection {
	Map<String, IOutputPipeline> map;
	public OutputPipelineCollection() {
		map=new HashMap<>();
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
