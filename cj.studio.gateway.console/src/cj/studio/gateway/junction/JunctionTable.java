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
	Map<String,Junction> inverts;
	IJunctionListener forwardListener;
	class CreateTimeComp implements Comparator<Junction>{

		@Override
		public int compare(Junction o1, Junction o2) {
			if(o1.createTime==o2.createTime)return 0;
			return o1.createTime>o2.createTime?1:-1;
		}
		
	}
	public JunctionTable() {
		this.forwards=new HashMap<>();
		this.inverts=new HashMap<>();
	}
	@Override
	public Junction[] toSortedForwards() {
		Junction[] ret=forwards.values().toArray(new Junction[0]);
		Arrays.sort(ret, new CreateTimeComp());
		return ret;
	}
	@Override
	public Junction[] toSortedInverts() {
		Junction[] ret=inverts.values().toArray(new Junction[0]);
		Arrays.sort(ret, new CreateTimeComp());
		return ret;
	}
	@Override
	public void add(Junction junction) {
		if(junction instanceof InvertJunction) {
			inverts.put(junction.name,junction);
			return;
		}
		forwards.put(junction.name,junction);
		if(forwardListener!=null) {
			forwardListener.monitor("A", junction);
		}
		
	}
	@Override
	public void remove(Junction junction) {
		if(junction instanceof InvertJunction) {
			inverts.remove(junction.name);
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
	public String[] enumInvertName() {
		return inverts.keySet().toArray(new String[0]);
	}
	@Override
	public int forwardsCount() {
		return forwards.size();
	}
	@Override
	public int invertsCount() {
		return inverts.size();
	}
	@Override
	public Junction findInForwards(String name) {
		return forwards.get(name);
	}
	@Override
	public Junction findInInverts(String name) {
		return forwards.get(name);
	}
	
	@Override
	public void addForwardListener(IJunctionListener listener) {
		this.forwardListener=listener;
	}
}
