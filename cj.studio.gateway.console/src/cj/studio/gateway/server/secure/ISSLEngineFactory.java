package cj.studio.gateway.server.secure;

import javax.net.ssl.SSLEngine;

public interface ISSLEngineFactory {
	void refresh();
	boolean isEnabled();
	SSLEngine createSSLEngine();
}
