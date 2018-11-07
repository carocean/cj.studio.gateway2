package cj.studio.gateway.junction;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import cj.studio.ecm.annotation.CjService;
import cj.studio.gateway.IJunctionTable;
@CjService(name="junctionTable")
public class JunctionTable implements IJunctionTable{
	Map<String,Junction> forwards;
	Map<String,Junction> backwards;
	IJunctionListener forwardListener;
	IJunctionListener backwardListener;
	class CreateTimeComp implements Comparator<Junction>{

		@Override
		public int compare(Junction o1, Junction o2) {
			if(o1.createTime==o2.createTime)return 0;
			return o1.createTime>o2.createTime?1:-1;
		}
		
	}
	public JunctionTable() {
		this.forwards=new HashMap<>();
		this.backwards=new HashMap<>();
	}
	@Override
	public Junction[] toSortedForwards() {
		Junction[] ret=forwards.values().toArray(new Junction[0]);
		Arrays.sort(ret, new CreateTimeComp());
		return ret;
	}
	@Override
	public Junction[] toSortedBackwards() {
		Junction[] ret=backwards.values().toArray(new Junction[0]);
		Arrays.sort(ret, new CreateTimeComp());
		return ret;
	}
	@Override
	public Junction[] toSortedAll() {
		Map<String,Object> map=new HashMap<>(forwards);
		map.putAll(backwards);
		Junction[] ret=map.values().toArray(new Junction[0]);
		Arrays.sort(ret, new CreateTimeComp());
		return ret;
	}
	@Override
	public void add(Junction junction) {
		if(junction instanceof BackwardJunction) {
			backwards.put(junction.name,junction);
			if(backwardListener!=null) {
				backwardListener.monitor("A", junction);
			}
			return;
		}
		forwards.put(junction.name,junction);
		if(forwardListener!=null) {
			forwardListener.monitor("A", junction);
		}
		
	}
	@Override
	public void remove(Junction junction) {
		if(junction instanceof BackwardJunction) {
			backwards.remove(junction.name);
			if(backwardListener!=null) {
				backwardListener.monitor("R", junction);
			}
			return;
		}
		forwards.remove(junction.name);
		if(forwardListener!=null) {
			forwardListener.monitor("R", junction);
		}
	}
	@Override
	public String[] enumForwardName() {
		return forwards.keySet().toArray(new String[0]);
	}
	@Override
	public String[] enumBackwardName() {
		return backwards.keySet().toArray(new String[0]);
	}
	@Override
	public int forwardsCount() {
		return forwards.size();
	}
	@Override
	public int backwardsCount() {
		return backwards.size();
	}
	@Override
	public Junction findInForwards(String name) {
		return forwards.get(name);
	}
	@Override
	public Junction findInBackwards(String name) {
		return backwards.get(name);
	}
	
	@Override
	public void addForwardListener(IJunctionListener listener) {
		this.forwardListener=listener;
	}
	@Override
	public void addBackwardListener(IJunctionListener listener) {
		this.backwardListener=listener;
	}
}
