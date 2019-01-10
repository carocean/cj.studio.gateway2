package cj.studio.gateway.socket.app.valve;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IChip;
import cj.studio.ecm.IChipInfo;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.script.IJssModule;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.socket.pipeline.IIPipeline;
import cj.studio.gateway.socket.pipeline.IInputValve;
import cj.ultimate.util.StringUtil;

public class LastWayInputValve implements IInputValve {
	public final static String SITE_HTTP_WELCOME = "site.http.welcome";
	public final static String SITE_DOCUMENT = "site.document";
	Map<String, Object> mappings;
	IGatewayAppSiteResource resource;
	String httpWelcome;
	private Pattern documentPattern;
	private Map<String, String> mimes;
	private IServiceProvider site;

	@SuppressWarnings("unchecked")
	public LastWayInputValve(IServiceProvider parent) {
		this.site = parent;
		this.mappings = (Map<String, Object>) parent.getService("$.app.create.webviews");
		this.mimes = (Map<String, String>) parent.getService("$.app.mimes");
		this.resource = (IGatewayAppSiteResource) parent.getService("$.app.create.resource");
		IChip chip = (IChip) parent.getService(IChip.class.getName());
		IChipInfo info = chip.info();
		String documentType = info.getProperty(SITE_DOCUMENT);
		String welcome = info.getProperty(SITE_HTTP_WELCOME);
		if (!StringUtil.isEmpty(welcome)) {
			if (welcome.indexOf(".") < 0) {
				throw new EcmException("HTTP_WELCOME不是文档");
			}
			while (welcome.startsWith("/")) {
				welcome = welcome.substring(1, welcome.length());
			}
		}
		this.httpWelcome = welcome;
		this.documentPattern = Pattern.compile(String.format("%s$", documentType.replace(".", ".*\\.")));
	}

	@Override
	public void onActive(String inputName, IIPipeline pipeline) throws CircuitException {
		pipeline.nextOnActive(inputName, this);
	}

	@Override
	public void flow(Object request, Object response, IIPipeline pipeline) throws CircuitException {
		Frame frame = (Frame) request;
		Circuit circuit = (Circuit) response;
		String rpath = frame.relativePath();
		String ext = frame.extName();
		if (!StringUtil.isEmpty(ext)) {// 如果有扩展名
			String dotext = String.format(".%s", ext);
			Matcher m = documentPattern.matcher(dotext);
			if (!m.matches()) {// 如果不是文档，则一定是资源
				renderResource(rpath, ext, frame, circuit);
				return;
			}
			// 余下的扩展名必是文档
			if (mappingsContainsKey(rpath)) {// 如果有java的webview则：
				renderJavaDocument(rpath, frame, circuit);
				return;
			}
			// 余下的则只可能是jss服务，故加载，如果不存在则报404
			boolean exists = renderJssDocument(rpath, frame, circuit);
			if (!exists) {
//				throw new CircuitException("404", "请求的文件不存在:" + frame.url());
				renderResource(rpath, ext, frame, circuit);// 如果jss也不存在则可能是资源，尝试加载一下，如果资源不存在则该方法会报404
			}
			return;
		}
		// 如果没有扩展名
		if (mappingsContainsKey(rpath)) {// 先看看文档中是否包含了
			renderJavaDocument(rpath, frame, circuit);
			return;
		}

		// 余下的如果路径以/结束必不是jss服务
		if (!rpath.endsWith("/")) {// 尝试加载jss服务
			boolean exists = renderJssDocument(rpath, frame, circuit);
			if (exists) {
				return;
			}
		}
		// 以上都试过了尝试按资源加载，如果不存在则在该方法中产生404
		renderResource(rpath, ext, frame, circuit);
	}

	private boolean renderJssDocument(String rpath, Frame frame, Circuit circuit) throws CircuitException {
		String filePath = "";
		int pos = rpath.lastIndexOf(".");
		if (pos > 0) {
			filePath = rpath.substring(0, pos);
		} else {
			filePath = rpath;
		}
		filePath = filePath.replace("/", ".");
		String jssSelectName = "";
		if (".".equals(filePath)) {
			jssSelectName = String.format("$.cj.jss.%s.index", IJssModule.FIXED_MODULENAME_HTTP_JSS);
		} else if (filePath.startsWith(".")) {
			if (filePath.endsWith(".")) {
				jssSelectName = String.format("$.cj.jss.%s%sindex", IJssModule.FIXED_MODULENAME_HTTP_JSS, filePath);
			} else {
				jssSelectName = String.format("$.cj.jss.%s%s", IJssModule.FIXED_MODULENAME_HTTP_JSS, filePath);
			}
		} else {
			if (filePath.endsWith(".")) {
				jssSelectName = String.format("$.cj.jss.%s%sindex", IJssModule.FIXED_MODULENAME_HTTP_JSS, filePath);
			} else {
				jssSelectName = String.format("$.cj.jss.%s.%s", IJssModule.FIXED_MODULENAME_HTTP_JSS, filePath);
			}
		}
		Object jssview = null;
		jssview = site.getService(jssSelectName);
		if (jssview != null) {
			if (!(jssview instanceof IGatewayAppSiteWayWebView)) {
				String message = String.format(
						"jss服务未定义webview接口,extends:'%s',必须声明为强jss类型：isStronglyJss:true,请求的jss服务：%s",
						IGatewayAppSiteWayWebView.class.getName(), jssSelectName);
				throw new CircuitException("503", message);
			}
			IGatewayAppSiteWayWebView view = (IGatewayAppSiteWayWebView) jssview;
			view.flow(frame, circuit, resource);
			if (!circuit.containsContentType()) {
				circuit.contentType("text/html; charset=utf-8");
			}
			return true;
		}
		return false;
	}

	private void renderJavaDocument(String rpath, Frame frame, Circuit circuit) throws CircuitException {
		IGatewayAppSiteWayWebView webview = (IGatewayAppSiteWayWebView) mappings.get(rpath);
		if (webview == null) {
			if (rpath.endsWith("/")) {
				rpath = rpath.substring(0, rpath.length() - 1);
			} else {
				rpath = String.format("%s/", rpath);
			}
			webview = (IGatewayAppSiteWayWebView) mappings.get(rpath);
		}
		webview.flow(frame, circuit, resource);
		if (!circuit.containsContentType()) {
			circuit.contentType("text/html; charset=utf-8");
		}
	}

	private void renderResource(String rpath, String ext, Frame frame, Circuit circuit) throws CircuitException {
		if (this.mimes.containsKey(ext)) {
			circuit.contentType(mimes.get(ext));
		}
//		try {
//			rpath=URLDecoder.decode(rpath, "utf-8");//为了不影响性能，资源中文名乱码问题留给应用开发者在valve中处理。
//		} catch (UnsupportedEncodingException e1) {
//		}
		if (rpath.endsWith("/")) {
			rpath = String.format("%s%s", rpath, httpWelcome);
		}
		File file = resource.realFileName(rpath);

		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			int read = 0;
			byte[] b = new byte[8192];
			while ((read = in.read(b)) != -1) {
				circuit.content().writeBytes(b,0,read);
			}
		} catch (FileNotFoundException e) {
			throw new CircuitException("404", e);
		} catch (IOException e) {
			throw new CircuitException("503", e);
		} finally {
			if(in!=null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}

	}

	private boolean mappingsContainsKey(String rpath) {
		if (mappings.containsKey(rpath)) {
			return true;
		}
		if (!"/".equals(rpath)) {
			if (rpath.endsWith("/")) {
				rpath = rpath.substring(0, rpath.length() - 1);
			} else {
				rpath = String.format("%s/", rpath);
			}
			if (mappings.containsKey(rpath)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onInactive(String inputName, IIPipeline pipeline) throws CircuitException {
		pipeline.nextOnInactive(inputName, this);

	}

}
