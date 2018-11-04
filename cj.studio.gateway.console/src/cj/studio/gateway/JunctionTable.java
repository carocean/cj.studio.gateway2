package cj.studio.gateway;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import cj.studio.ecm.annotation.CjService;
@CjService(name="junctionTable")
public class JunctionTable implements IJunctionTable{
	List<Junction> forwards;
	List<Junction> inverts;
	public JunctionTable() {
		this.forwards=new ArrayList<>();
		this.inverts=new ArrayList<>();
	}
	@Override
	public void add(Junction junction) {
		if(junction instanceof InvertJunction) {
			inverts.add(junction);
			return;
		}
		forwards.add(junction);
		
	}
	@Override
	public void remove(Junction junction) {
		if(junction instanceof InvertJunction) {
			inverts.remove(junction);
			return;
		}
		forwards.remove(junction);
	}
	@Override
	public Junction getInForwards(int index) {
		return forwards.get(index);
	}
	@Override
	public Junction getInInverts(int index) {
		return inverts.get(index);
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
		Junction[] a=forwards.toArray(new Junction[0]);
		JunctionComparator comparator=new JunctionComparator();
		Junction junction=new ForwardJunction(name);
		int index=Arrays.binarySearch(a, junction, comparator);
		if(index<0)return null;
		return forwards.get(index);
	}
	@Override
	public Junction findInInverts(String name) {
		Junction[] a=forwards.toArray(new Junction[0]);
		JunctionComparator comparator=new JunctionComparator();
		Junction junction=new InvertJunction(name);
		int index=Arrays.binarySearch(a, junction, comparator);
		if(index<0)return null;
		return forwards.get(index);
	}
	class JunctionComparator implements Comparator<Junction>{

		@Override
		public int compare(Junction o1, Junction o2) {
			return o1.name.compareTo(o2.name);
		}
		
	}
}
