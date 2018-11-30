package cj.studio.gateway.socket.app;

import cj.studio.ecm.INamedProvider;

/**
 * 网关app插件<br>
 * 注意：插件的加载器依赖于app程序集的references中的jar，故而应在运行期将插件的api存根放入app程序集的cj.references下，插件只在编译期引用存根即可。
 * 对于相同的引用包，优先加载app程序集references中的，因此在app中可直接调用和转换插件中在的对象。
 * 
 * <pre>
 * 
 * </pre>
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
