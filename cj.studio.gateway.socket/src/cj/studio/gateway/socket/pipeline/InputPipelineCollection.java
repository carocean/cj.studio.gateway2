package cj.studio.gateway.socket.pipeline;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cj.ultimate.IDisposable;

public class InputPipelineCollection implements IDisposable{
	Map<String, IInputPipeline> map;//key是dest目标
	public InputPipelineCollection() {
		map=new ConcurrentHashMap<>();
	}
	public Set<String> enumDest(){
		return map.keySet();
	}
	public boolean containsName(String dest) {
		return map.containsKey(dest);
	}
	public IInputPipeline get(String dest) {
		return map.get(dest);
	}
	public void add(String dest,IInputPipeline pipeline) {
		map.put(dest, pipeline);
	}
	public void remove(String dest) {
		map.remove(dest);
	}
	public boolean isEmpty() {
		return map.isEmpty();
	}
	@Override
	public void dispose() {
		map.clear();	}
}
