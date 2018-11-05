package cj.studio.gateway.socket.pipeline;
/**
 * 必须声明为多例服务
 * @author caroceanjofers
 *
 */
public interface ICustomOutputValve extends IOutputValve {
	int getSort();
}
