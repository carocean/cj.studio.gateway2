package cj.studio.gateway.junction;

import java.util.HashMap;
import java.util.Map;

import cj.studio.ecm.annotation.CjService;
import cj.studio.gateway.IJunctionTable;
@CjService(name="junctionTable")
public class JunctionTable implements IJunctionTable{
	Map<String,Junction> forwards;
	Map<String,Junction> inverts;
	IJunctionListener forwardListener;
	public JunctionTable() {
		this.forwards=new HashMap<>();
		this.inverts=new HashMap<>();
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
