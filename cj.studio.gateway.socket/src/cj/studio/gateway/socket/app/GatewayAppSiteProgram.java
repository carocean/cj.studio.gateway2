package cj.studio.gateway.socket.app;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IChip;
import cj.studio.ecm.IChipInfo;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceSite;
import cj.studio.ecm.logging.ILogging;
import cj.studio.ecm.net.layer.ISessionEvent;
import cj.studio.ecm.script.IJssModule;
import cj.studio.gateway.socket.Destination;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import cj.studio.gateway.socket.pipeline.InputPipeline;
import cj.studio.gateway.socket.valve.CheckErrorInputVavle;
import cj.studio.gateway.socket.valve.CheckSessionInputValve;
import cj.studio.gateway.socket.valve.CheckUrlInputValve;
import cj.studio.gateway.socket.valve.FirstJeeInputValve;
import cj.studio.gateway.socket.valve.FirstWayInputValve;
import cj.studio.gateway.socket.valve.LastJeeInputValve;
import cj.studio.gateway.socket.valve.LastWayInputValve;

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
		// 初始化会话事件
		IAppSiteSessionManager sessionManager = (IAppSiteSessionManager) site.getService("$.sessionManager");
		List<ISessionEvent> events = getSessionEvents();
		if (events != null) {
			sessionManager.getEvents().addAll(events);
		}
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
	
	protected List<ISessionEvent> getSessionEvents(){
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

	@Override
	public final IInputPipeline createInputPipeline(String name) {
		IInputPipeline input = null;
		if (type == ProgramAdapterType.jee) {
			FirstJeeInputValve first=new FirstJeeInputValve();
			LastJeeInputValve last=new LastJeeInputValve();
			
			input=new InputPipeline(first, last);
		} else {
			//注意顺序
			FirstWayInputValve first = new FirstWayInputValve(0);
			LastWayInputValve last = new LastWayInputValve(this);
			input = new InputPipeline(first, last);
			CheckUrlInputValve checkuri = createCheckUriEndWithDirSymbol();
			if (checkuri != null) {
				input.add(checkuri);
			}
			
			CheckErrorInputVavle error=new CheckErrorInputVavle(this);
			input.add(error);//添在此位置,error将产生会话，如果不想产生会话可放在sessionValve后面
			
			CheckSessionInputValve session = new CheckSessionInputValve(this);
			input.add(session);
			
		}
		
		return input;
	}

	/**
	 * 检查无扩展名请求路径是否在结尾处有目录符号，如果没有则通知客户端重定向
	 * 
	 * @return
	 */
	protected CheckUrlInputValve createCheckUriEndWithDirSymbol() {
		return new CheckUrlInputValve();
	}

}
