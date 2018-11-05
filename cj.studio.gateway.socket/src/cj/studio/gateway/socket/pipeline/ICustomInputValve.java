package cj.studio.gateway.socket.pipeline;
/**
 * 必须声明为多例服务
 * @author caroceanjofers
 *
 */
public interface ICustomInputValve extends IInputValve {
	int getSort();
}
