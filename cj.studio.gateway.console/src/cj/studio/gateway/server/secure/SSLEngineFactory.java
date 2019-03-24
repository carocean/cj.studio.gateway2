package cj.studio.gateway.server.secure;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.Security;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.gateway.conf.ServerInfo;
import cj.ultimate.util.StringUtil;

public class SSLEngineFactory implements ISSLEngineFactory {
	private static final String PROTOCOL = "TLS";
	IServiceProvider parent;
	ServerInfo info;
	SSLContext serverContext;

	public SSLEngineFactory(IServiceProvider parent) {
		this.parent = parent;
		info = (ServerInfo) parent.getService("$.server.info");
	}

	@Override
	public boolean isEnabled() {
		return "true".equals(info.getProps().get("SSL-Enabled"));
	}

	@Override
	public void refresh() {
		if (!isEnabled()) {
			return;
		}
		String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
		if (algorithm == null) {
			algorithm = "SunX509";
		}
		String keyStoreFile = info.getProps().get("KeyStore-File");
		if (StringUtil.isEmpty(keyStoreFile)) {
			throw new EcmException("未设置server的属性：KeyStore-File");
		}
		if (keyStoreFile.startsWith("~/")) {
			String home = (String) parent.getService("$.homeDir");
			keyStoreFile = keyStoreFile.substring(2, keyStoreFile.length());
			keyStoreFile = String.format("%s%s%s", home,File.separator, keyStoreFile);
		}
		String keyStoreType = info.getProps().get("KeyStore-Type");
		if (StringUtil.isEmpty(keyStoreType)) {
			keyStoreType = "JKS";
		}
		String keyStorePwd = info.getProps().get("KeyStore-Password");
		if (StringUtil.isEmpty(keyStorePwd)) {
			throw new EcmException("未设置server的属性：KeyStore-Password");
		}
		String certPwd = info.getProps().get("Cert-Password");
		if (StringUtil.isEmpty(certPwd)) {
			throw new EcmException("未设置server的属性：Cert-Password");
		}
		FileInputStream in = null;
		try {
			in = new FileInputStream(keyStoreFile);
			KeyStore ks = KeyStore.getInstance(keyStoreType);
			ks.load(in, keyStorePwd.toCharArray());

			// Set up key manager factory to use our key store
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
			kmf.init(ks, certPwd.toCharArray());

			// Initialize the SSLContext to work with our key managers.
			serverContext = SSLContext.getInstance(PROTOCOL);
			serverContext.init(kmf.getKeyManagers(), null, null);
		} catch (Exception e) {
			throw new Error("Failed to initialize the server-side SSLContext", e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}

	}

	@Override
	public SSLEngine createSSLEngine() {
		return serverContext.createSSLEngine();
	}
}
