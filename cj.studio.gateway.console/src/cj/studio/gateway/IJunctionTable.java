package cj.studio.gateway;

import cj.studio.gateway.junction.IJunctionListener;
import cj.studio.gateway.junction.Junction;

/**
 * 交合点，有输入交合和输出交合
 * @author caroceanjofers
 *
 */
public interface IJunctionTable {

	void add(Junction junction);

	void remove(Junction junction);
	String[] enumForwardName();
	String[] enumBackwardName();

	int forwardsCount();

	int backwardsCount();
	Junction[] toSortedForwards();
	Junction[] toSortedBackwards();
	Junction findInBackwards(String name);

	Junction findInForwards(String name);

	void addForwardListener(IJunctionListener listener);

	void addBackwardListener(IJunctionListener listener);

	Junction[] toSortedAll();

}
