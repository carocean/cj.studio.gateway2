package cj.studio.gateway.socket.pipeline;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cj.ultimate.IDisposable;

public class InputPipelineCollection implements IDisposable{
	Map<String, IInputPipeline> map;
	public InputPipelineCollection() {
		map=new HashMap<>();
	}
	public Set<String> enumName(){
		return map.keySet();
	}
	public boolean containsName(String name) {
		return map.containsKey(name);
	}
	public IInputPipeline get(String name) {
		return map.get(name);
	}
	public void add(String name,IInputPipeline pipeline) {
		map.put(name, pipeline);
	}
	public void remove(String name) {
		map.remove(name);
	}
	public boolean isEmpty() {
		return map.isEmpty();
	}
	@Override
	public void dispose() {
		map.clear();	}
}
