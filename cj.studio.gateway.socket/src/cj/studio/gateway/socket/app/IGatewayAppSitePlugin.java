package cj.studio.gateway.socket.app;

import cj.studio.ecm.INamedProvider;

/**
 * 网关app插件
 * 
 * @author caroceanjofers
 *
 */
public interface IGatewayAppSitePlugin extends INamedProvider {
	/**
	 * 插件的服务获取格式是：插件所有程序集的assemblyTitle.插件内服务名
	 */
	@Override
	Object getService(String arg0);
}
