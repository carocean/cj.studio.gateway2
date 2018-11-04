package cj.studio.gateway.server.util;

import java.util.Map;

import cj.studio.ecm.EcmException;
import cj.ultimate.util.StringUtil;

public class GetwayDestHelper {

	public static String getGatewayDestForHttpRequest(String uri, String gatewayDestInHeader, Map<String, String> props,
			Class<?> clazz) {
		String gatewayDest = "";
		if (!StringUtil.isEmpty(gatewayDestInHeader)) {
			int pos = gatewayDestInHeader.indexOf(",");
			if (pos >= 0) {
				throw new EcmException("http协议不支持多级目标路由，关键头：Gateway-Dest:" + gatewayDestInHeader);
			}
			return gatewayDest;
		}
		if ("/".equals(uri)) {
			return gatewayDest;
		}
		// 如果没指定gatewayDest可能是向app目标发起的请求，则取上下文作为目标
		String cntpath = uri;
		while(cntpath.indexOf("/")==0) {
			cntpath = cntpath.substring(1, cntpath.length());
		}
		int pos = cntpath.indexOf("/");
		if (pos > 0) {
			cntpath = cntpath.substring(0, pos);
		} else {
			if (cntpath.lastIndexOf(".") > -1) {
				cntpath = "";
			}
		}
		gatewayDest = cntpath;
		return gatewayDest;
	}

}
