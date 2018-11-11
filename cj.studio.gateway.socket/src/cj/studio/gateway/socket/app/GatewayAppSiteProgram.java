package cj.studio.gateway.socket.app;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IChip;
import cj.studio.ecm.IChipInfo;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.Scope;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceSite;
import cj.studio.ecm.logging.ILogging;
import cj.studio.ecm.net.layer.ISessionEvent;
import cj.studio.ecm.script.IJssModule;
import cj.studio.gateway.socket.Destination;

public abstract class GatewayAppSiteProgram implements IGatewayAppSiteProgram {
	static ILogging logger = CJSystem.logging();
	@CjServiceSite
	IServiceSite site;
	ProgramAdapterType type;
	private Map<String, String> errors;
	private Map<String, String> mimes;
	private String http_root;

	@Override
	public final Object getService(String name) {
		if ("$.app.site".equals(name)) {
			return site;
		}
		if ("$.app.create.webviews".equals(name)) {
			Map<String, Object> mappings = getWebviewMappings();
			return mappings;
		}
		if ("$.app.create.resource".equals(name)) {
			GatewayAppSiteResource resource = new GatewayAppSiteResource(http_root);
			return resource;
		}
		if ("$.app.mimes".equals(name)) {
			return mimes;
		}
		if ("$.app.errors".equals(name)) {
			return errors;
		}
		if ("$.app.type".equals(name)) {
			return type;
		}
		if ("$.session.events".equals(name)) {
			return getSessionEvents();
		}
		return site.getService(name);
	}

	@Override
	public <T> ServiceCollection<T> getServices(Class<T> arg0) {
		return site.getServices(arg0);
	}

	@Override
	public void close() {
		site = null;
		this.mimes.clear();
		this.errors.clear();
	}

	@Override
	public final void start(Destination dest, String assembliesHome, ProgramAdapterType type) {
		this.type = type;
		mimes = new HashMap<>();
		errors = new HashMap<>();

		IChip chip = (IChip) site.getService(IChip.class.getName());
		IChipInfo info = chip.info();

		parseErrors(info);
		parseMimes(info);

		ClassLoader sysres = this.getClass().getClassLoader();
		ClassLoader oldcl = memoClassloader(sysres);

		this.http_root = String
				.format("%s/%s/%s", info.getProperty("home.dir"), IJssModule.RUNTIME_SITE_DIR,
						info.getResourceProp("http.root"))
				.replace("///", "/").replace("//", "/").replace("/", File.separator);

		try {
			onstart(dest, assembliesHome, type);
		} finally {
			redoClassloader(oldcl);
		}
	}

	protected List<ISessionEvent> getSessionEvents() {
		return null;
	}

	private Map<String, Object> getWebviewMappings() {
		ServiceCollection<?> col = null;
		if (type == ProgramAdapterType.jee) {
			col = site.getServices(IGatewayAppSiteJeeWebView.class);
		} else {
			col = site.getServices(IGatewayAppSiteWayWebView.class);
		}
		Map<String, Object> mappings = new HashMap<>();
		for (Object view : col) {
			if (view.getClass().getSimpleName().endsWith("$$NashornJavaAdapter")) {
				continue;
			}
			CjService cs = view.getClass().getAnnotation(CjService.class);
			if (cs == null) {
				logger.error(getClass(), String.format("映射错误：%s 未有CjService注解", view));
				continue;
			}
			if (cs.scope() == Scope.singleon) {
				logger.warn(getClass(), String.format("webview被声明为Scope.singleon模式，推荐将webview声明为多例或运行时服务。webview：%s,在 %s", cs.name(),
						view.getClass(), view));
			}
			String name = cs.name();
			if (!name.startsWith("/")) {
				name = String.format("/%s", name);
			}
			if (name.lastIndexOf(".") == -1 && !name.endsWith("/")) {
				name = String.format("%s/", name);
			}
			mappings.put(name, view);
		}
		return mappings;
	}

	protected abstract void onstart(Destination dest, String assembliesHome, ProgramAdapterType type);

	private void redoClassloader(ClassLoader oldcl) {
		Thread.currentThread().setContextClassLoader(oldcl);
	}

	private ClassLoader memoClassloader(ClassLoader classLoader) {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(classLoader);
		return cl;
	}

	private void parseErrors(IChipInfo info) {
		String[] keys = info.enumProperty();
		for (String key : keys) {
			int pos = key.indexOf("site.http.error.");
			if (pos == 0) {
				String k = key.substring("site.http.error.".length(), key.length());
				String v = info.getProperty(key);
				errors.put(k, v);
			}
		}
	}

	private void parseMimes(IChipInfo info) {
		String[] keys = info.enumProperty();
		for (String key : keys) {
			int pos = key.indexOf("site.http.mime.");
			if (pos == 0) {
				String k = key.substring("site.http.mime.".length(), key.length());
				String v = info.getProperty(key);
				mimes.put(k, v);
			}
		}
	}

}
