package cj.studio.gateway.socket.app;

import java.io.File;

import org.jsoup.nodes.Document;

import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;

public interface IGatewayAppSiteResource {
	public Document html(String relativedUrl) throws CircuitException;
	public void redirect(String relativedUrl, Circuit circuit);
	public Document html(String relativedUrl, String charset)
			throws CircuitException ;
	public Document html(String relativePath, String resourceRefix,
			String charset)throws CircuitException ;
	public String resourceText(String relativedUrl) throws CircuitException;
	public byte[] resource(String relativedUrl) throws CircuitException;
	String getRealHttpSiteRootPath();
	public File realFileName(String rpath)throws CircuitException;
}
