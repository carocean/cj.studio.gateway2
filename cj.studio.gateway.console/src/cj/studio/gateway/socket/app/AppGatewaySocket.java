package cj.studio.gateway.socket.app;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cj.studio.ecm.Assembly;
import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.IAssembly;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.IWorkbin;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.session.ISessionEvent;
import cj.studio.ecm.resource.JarClassLoader;
import cj.studio.gateway.ICluster;
import cj.studio.gateway.IConfiguration;
import cj.studio.gateway.IGatewaySocketContainer;
import cj.studio.gateway.IRuntime;
import cj.studio.gateway.socket.Destination;
import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.app.pipeline.builder.AppSocketInputPipelineBuilder;
import cj.studio.gateway.socket.app.pipeline.builder.AppSocketOutputPipelineBuilder;
import cj.studio.gateway.socket.pipeline.IInputPipelineBuilder;
import cj.studio.gateway.socket.pipeline.IOutputPipelineBuilder;
import cj.studio.gateway.socket.pipeline.IOutputSelector;
import cj.studio.gateway.socket.pipeline.OutputSelector;
import cj.ultimate.IDisposable;
import cj.ultimate.gson2.com.google.gson.Gson;
import cj.ultimate.util.StringUtil;

public class AppGatewaySocket implements IGatewaySocket, IServiceProvider {

	private IServiceProvider parent;
	private Destination destination;
	private IInputPipelineBuilder inputBuilder;
	private IOutputPipelineBuilder outputBuilder;
	private boolean isConnected;
	private String homeDir;
	private IGatewayAppSiteProgram program;
	IAppSiteSessionManager sessionManager;
	List<String> runtimeAddedDestNames;
	public AppGatewaySocket(IServiceProvider parent) {
		this.parent = parent;
		inputBuilder = new AppSocketInputPipelineBuilder(this);
		outputBuilder = new AppSocketOutputPipelineBuilder(this);
		this.homeDir = (String) parent.getService("$.homeDir");
		this.runtimeAddedDestNames=new ArrayList<>();
	}

	public boolean isConnected() {
		return isConnected;
	}

	@Override
	public Object getService(String name) {
		if ("$.pipeline.input.builder".equals(name)) {
			return inputBuilder;
		}
		if ("$.pipeline.output.builder".equals(name)) {
			return outputBuilder;
		}
		if ("$.app.program".equals(name)) {
			return program;
		}
		if ("$.socket".equals(name)) {
			return this;
		}
		if ("$.sessionManager".equals(name)) {
			return this.sessionManager;
		}
		if ("$.destination".equals(name)) {
			return this.destination;
		}
		if ("$.socket.name".equals(name)) {
			return this.name();
		}
		if ("$.localAddress".equals(name)) {
			return new Gson().toJson(destination.getUris());
		}
		return parent.getService(name);
	}

	@Override
	public <T> ServiceCollection<T> getServices(Class<T> clazz) {
		return parent.getServices(clazz);
	}

	@Override
	public String name() {
		return destination.getName();
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized void connect(Destination dest) throws CircuitException {
		if (isConnected) {
			return;
		}
		this.destination = dest;
		String uri = dest.getUris().get(0);

		String port = uri.substring(uri.lastIndexOf(":") + 1, uri.length()).trim();
		ProgramAdapterType type = ProgramAdapterType.valueOf(port);
		// uri格式是：app://目录:适配类型,如：app://程序的根目录相对于容器下目录assembly的位置:适配类型，例：app://wigo:way
		String appdir = uri.substring(uri.indexOf("://") + 3, uri.lastIndexOf(":"));

		String sharedir = String.format("%s%slib%sshare", homeDir, File.separator, File.separator);
		ClassLoader share = FillClassLoader.fillShare(sharedir);
		CJSystem.current().environment().logging().info("已装载共享库" + sharedir);

		String assembliesHome = String.format("%s%sassemblies%s%s%s", homeDir, File.separator, File.separator, appdir,
				File.separator);
		Object prog = scanAssemblyAndLoad(assembliesHome, share);

		IGatewayAppSiteProgram wprog = (IGatewayAppSiteProgram) prog;
		// 初始化会话事件
		List<ISessionEvent> events = (List<ISessionEvent>) wprog.getService("$.session.events");
		if (events != null) {
			sessionManager.getEvents().addAll(events);
		}

		wprog.start(dest, assembliesHome, type);
		isConnected = true;
	}

	protected Object scanAssemblyAndLoad(String home, ClassLoader share) throws CircuitException {
		File dir = new File(home);
		if (!dir.exists()) {
			throw new EcmException("程序集目录不存在:" + dir);
		}
		File[] assemblies = dir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".jar");
			}
		});
		if (assemblies.length == 0) {
			throw new EcmException("缺少程序集:" + home);
		}
		if (assemblies.length > 1) {
			throw new EcmException("定义了多个程序集:" + home);
		}
		String fn = assemblies[0].getAbsolutePath();

		IAssembly target = Assembly.loadAssembly(fn, share);
		Map<String, IGatewayAppSitePlugin> plugins = scanPluginsAndLoad(home, target.info().getReferences(), share);
		target.parent(new AppCoreService(plugins));

		target.start();

		this.program = (IGatewayAppSiteProgram) target.workbin().part("$.cj.studio.gateway.app");

		if (program == null) {
			throw new EcmException("程序集验证失败，原因：未发现Program的派生实现");
		}

		String expire = target.info().getProperty("site.session.expire");
		if (StringUtil.isEmpty(expire)) {
			expire = (30 * 60 * 1000L) + "";
		}
		sessionManager = new AppSiteSessionManager(Long.valueOf(expire));
		sessionManager.start();

		return program;
	}

	private Map<String, IGatewayAppSitePlugin> scanPluginsAndLoad(String assemblyHome, ClassLoader pcl,
			ClassLoader share) {
		String dir = assemblyHome;
		if (!dir.endsWith(File.separator)) {
			dir += File.separator;
		}
		dir = String.format("%splugins", dir);
		File dirFile = new File(dir);
		if (!dirFile.exists()) {
			dirFile.mkdirs();
		}
		File[] pluginDirs = dirFile.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
		Map<String, IGatewayAppSitePlugin> map = new HashMap<>();
		if (pluginDirs.length == 0) {
			return map;
		}
		for (File f : pluginDirs) {
			File[] pluginFiles = f.listFiles(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".jar");
				}
			});
			for (File pluginFile : pluginFiles) {
				JarClassLoader parent = new JarClassLoader(pcl);
				IAssembly assembly = Assembly.loadAssembly(pluginFile.getAbsolutePath(), parent);
				assembly.parent(new AppCoreService(new HashMap<>()));
				assembly.start();
				IWorkbin bin = assembly.workbin();
				IGatewayAppSitePlugin plugin = (IGatewayAppSitePlugin) bin.part("$.studio.gateway.app.plugin");
				if (plugin != null) {
					String name = bin.chipInfo().getName();
					map.put(name, plugin);
				}
			}
		}
		return map;
	}

	@Override
	public void close() throws CircuitException {
		IDisposable runtime=(IDisposable)this.program.getService("$.gateway.runtime");
		runtime.dispose();
		IGatewaySocketContainer container = (IGatewaySocketContainer) parent.getService("$.container.socket");
		if (container != null) {
			container.remove(name());
		}
		isConnected = false;
		sessionManager.stop();
		program.close();
		this.runtimeAddedDestNames.clear();
		this.inputBuilder = null;
		this.parent = null;
		this.program = null;
		this.sessionManager = null;
		this.runtimeAddedDestNames=null;
		
	}

	class AppCoreService implements IServiceProvider {
		IOutputSelector selector;
		Map<String, IGatewayAppSitePlugin> plugins;

		public AppCoreService(Map<String, IGatewayAppSitePlugin> plugins) {
			this.plugins = plugins;
		}

		@Override
		public Object getService(String name) {
			if ("$.output.selector".equals(name)) {
				if (selector == null) {
					selector = new OutputSelector(AppGatewaySocket.this);
				}
				return selector;
			}
			
			if (!plugins.isEmpty()) {
				int pos = name.indexOf(".");
				if (pos > 0) {
					String key = name.substring(0, pos);
					String sid = name.substring(pos + 1, name.length());
					IGatewayAppSitePlugin plugin = plugins.get(key);
					if (plugin != null) {
						Object obj = plugin.getService(sid);
						if (obj != null)
							return obj;
					}
				}
			}
			if("$.gateway.runtime".equals(name)) {
				IConfiguration config =(IConfiguration)parent.getService("$.config");
				return new Runtime(config);
			}
			return null;
		}

		@Override
		public <T> ServiceCollection<T> getServices(Class<T> arg0) {
			// TODO Auto-generated method stub
			return null;
		}

	}
	
	class Runtime implements IRuntime,IDisposable{
		ICluster cluster;
		IConfiguration config;
		public Runtime(IConfiguration config) {
			this.cluster=config.getCluster();
			this.config=config;
		}
		@Override
		public void dispose() {
			String[] names=runtimeAddedDestNames.toArray(new String[0]);
			for(String name:names) {
				removeDestination(name);
			}
		}
		@Override
		public void flushCluster() {
			config.flushCluster();
		}
		@Override
		public void addDestination(Destination dest) {
			if(StringUtil.isEmpty(dest.getName())||dest.getUris().isEmpty()) {
				throw new EcmException("目标缺少目标名或远程地址");
			}
			dest.getProps().put("Is-Runtime-Destination", "true");
			cluster.addDestination(dest);
			runtimeAddedDestNames.add(dest.getName());
		}

		@Override
		public void removeDestination(String domain) {
			cluster.removeDestination(domain);
			runtimeAddedDestNames.remove(domain);
		}

		@Override
		public Destination getDestination(String domain) {
			return cluster.getDestination(domain);
		}

		@Override
		public boolean containsValid(String domain) {
			return cluster.containsValid(domain);
		}

		@Override
		public void validDestination(String domain) {
			cluster.validDestination(domain);
		}

		@Override
		public boolean containsInvalid(String domain) {
			return cluster.containsInvalid(domain);
		}

		@Override
		public void invalidDestination(String domain, String cause) {
			cluster.invalidDestination(domain, cause);
		}
		
	}
}
