package cj.studio.gateway;
/**
 * 交合点，有输入交合和输出交合
 * @author caroceanjofers
 *
 */
public interface IJunctionTable {

	void add(Junction junction);

	void remove(Junction junction);

	Junction getInForwards(int index);

	Junction getInInverts(int index);

	int forwardsCount();

	int invertsCount();

	Junction findInInverts(String name);

	Junction findInForwards(String name);

}
