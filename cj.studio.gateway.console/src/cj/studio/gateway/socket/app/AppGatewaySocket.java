package cj.studio.gateway.socket.app;

import java.io.File;
import java.io.FilenameFilter;

import cj.studio.ecm.Assembly;
import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.IAssembly;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.Destination;
import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.InputPipelineBuilder;
import cj.studio.gateway.socket.app.session.AppSiteSessionManager;
import cj.studio.gateway.socket.pipeline.IInputPipelineBuilder;
import cj.ultimate.util.StringUtil;

public class AppGatewaySocket implements IGatewaySocket, IServiceProvider {

	private IServiceProvider parent;
	private Destination destination;
	private IInputPipelineBuilder builder;
	private boolean isConnected;
	private String homeDir;
	private IGatewayAppSiteProgram program;
	IAppSiteSessionManager sessionManager;
	public AppGatewaySocket(IServiceProvider parent) {
		this.parent = parent;
		builder = new InputPipelineBuilder((IServiceProvider) this);
		this.homeDir = (String) parent.getService("$.homeDir");
	}

	public boolean isConnected() {
		return isConnected;
	}

	@Override
	public Object getService(String name) {
		if ("$.pipeline.input.builder".equals(name)) {
			return builder;
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

	@Override
	public void connect(Destination dest) throws CircuitException {
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
		IServiceSite site=(IServiceSite)wprog.getService("$.app.site");
		site.addService("$.sessionManager", sessionManager);//将会话管理器注入到app中
		wprog.start(dest, assembliesHome,type);
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
		target.start();

		this.program = (IGatewayAppSiteProgram) target.workbin().part("$.cj.studio.gateway.app");

		if (program == null) {
			throw new EcmException("程序集验证失败，原因：未发现Program的派生实现");
		}
		String expire=target.info().getProperty("site.session.expire");
		if(StringUtil.isEmpty(expire)) {
			expire=(30*60*1000L)+"";
		}
		sessionManager=new AppSiteSessionManager(Long.valueOf(expire));
		sessionManager.start();
		
		return program;
	}

	@Override
	public void close() throws CircuitException {
		isConnected = false;
		sessionManager.stop();
		program.close();
		this.builder=null;
		this.parent=null;
		this.program=null;
		this.sessionManager=null;
	}

}
