
# Gateway2.0

## 示例工程参见我的github的另外两外项目：cj.studio.gateway.examples.frontend，cj.studio.gateway.examples.backend
	已实现将Multipart的请求以内存的方式解析，但为了避免像tomcat/jetty这样的web容器存在的大文件上传上限问题和大文件上传必须接收到磁盘缓冲的问题，这种规避内存使用的方法性能和灵活性都不好。
	gateway2.0要将数据接口开放给开发者，由开发者决定如何做，中间不做任何缓冲，这样便实现了文件无限制上传能力。
	接下来还要实现的功能有：
- http server：用于在多级网关中转发http块，中间不做任何和处理，这类似于ngnix和apache的功能。
- 订阅式广播应用：多个网关向一个目标网关主题应用凝聚，主题收到消息即向各订阅结点分发，主题可由运维人员自建。该功能类似于memcache和zookeeper，其用途在于各网关节点的同步。
- 为各种net协议加心跳机制。	
- 微服务定义和客端调用机制，包括向指网关注册（任意网关均可作为微服务中心）。由于注册（报送微服务信息）、路由、发现、断路重连是网关的基本功能，所以微服务机制的实现可在2个工作日内完成。
	
	Gateway即可用于开发网站项目，也可用于开发微服务项目，称之为网关应用，在作为分布式平台使用时，支持微服务的注册、发现、路由、断路重试等功能。它可以发展为N层分布式架构，层级由运维人员自建。
 	Gateway对于中小型互联网公司来说是一种福音，因为这类公司往往没有实力或者没有足够的资金投入去开发自己的分布式系统基础设施，使用Gateway一站式解决方案能在从容应对业务发展的同时大大减少开发成本。同时，随着近几年微服务架构和Docker容器概念的火爆，也会让Gateway在未来越来越“云”化的软件开发风格中立有一席之地，尤其是在目前五花八门的分布式解决方案中提供了标准化的、全站式的技术方案，意义可能会堪比当年Servlet规范的诞生，有效推进服务端软件系统技术水平的进步。
 	
 	cj.studio还有与之配套的以下产品:
 	
 	ecm开发工具包兼有spring、osgi、nodejs的功能。支持面向模块开发与部署，热插拔。 	
	net开发工具包,支持web的开发，并且可以完全使用js开发web应用，它的语法更接近于流行的nodejs，其它功能包含有基于netty的nio也包含有自行开发的nio框架rio,rio叫响应式nio框架，它即有nio的优势，又有同步等待响应的能力。
	plus开发工具包，进一步增强了连接的能力，如web应用的插件化（支持像eclipse这样的插件架构），支持像webservice这种远程服务的能力，支持云计算芯片的开发。
	netdisk是基于mongodb的增强开发工具包，它实现了网盘的各种功能，支持文件随机存取及结构化数据存取，支持多维，用户配额，开发上类似sql语法支持、对象映射支持。
	neuron工具，具有像tomcat/jetty等服务容器的功能，更多的是它具有向后连接的特性，是组建大型分布式神经网络的节点工具。它的目的就是组建神经网络集群。
	mdisk命令行工具，它是以命令行窗口实现的网盘工具，以netdisk为核心，方便mongodb的开发、测试和运维管理。它用起来非常简单，只要连到你的mongodb即可将mongodb当成网盘数据库，且对原mongodb的库不受影响。
	cjnet 用于调试neuron中的应用程序和netsite中的应用程序，它是一个cj studio产品系中有关net产品开发和调试必不可少的工具。
	netsite也是一个像tomcat/jetty等服务容器的命令行工具，它与神经元的区别在于，它只能部署在神经网络的终端，而不能成为其中间节点。它的优点在于，它可以部署成百上千个应用，而在一个神经元节点上一般不这么做。此工具暂时停止了升级。
## 与nginx兼容
* 在使用默认的nginx配置时会报错误：upstream prematurely closed connection while reading upstream
* gateway不支持http/1.0协议，如以1.0协议请求会被拒绝，而nginx的默认使用的是http/1.0协议(nginx不管浏览器发来的请求是否是http/1.1都默认改写为http/1.0发应用发请求)，因此必须修改为支持http/1.1，如下：

```nginx

upstream website{
    server localhost:8080;
    keepalive 65; #必须加上
}
location /website/ {
    proxy_pass http://website;
    proxy_http_version 1.1;#必须加上
    proxy_set_header Connection "";#必须加上，此处nginx覆盖了请求头中的Connection值，如果能取到最好，我还不知道怎么配置获取
    proxy_set_header Host $host:$server_port;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}
#这是路由websocket
location /myChannel {
    proxy_pass http://website;
    proxy_http_version 1.1;#必须加上
    proxy_set_header Upgrade $http_upgrade;#必须加上
    proxy_set_header Connection "keep-alive, Upgrade";#必须加上
    proxy_set_header Host $host:$server_port;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}

```

## 网关应用的调试方法
   有两种调试方法，一种是建立main项目，此方法适用于eclipse和idea开发工具；另一种是直接使用idea的Jar Application，该方法仅适用于intellij idea开发工具
   - 建立main项目调试
   1.建立项目，将下面代码考贝进去
   复制代码：
   ```java
public class AppMain {


	private static String fileName;
	public static void main(String[] args) throws ParseException, IOException {
		fileName = "cj.studio.gateway.console";
		Options options = new Options();
//		Option h = new Option("h", "host",false, "要绑定的ip地址（一台服务器上可能有多网卡，默认采用localhost)，格式：-h ip:port，port可以省去");
//		options.addOption(h);
		Option  l = new Option("l","log", false, "充许网络日志输出到控制台");
		options.addOption(l);
		Option  m = new Option("m","man", false, "帮助");
		options.addOption(m);
		Option  u = new Option("nohup","nohup", false, "使用nohup后台启动");
		options.addOption(u);
//		Option  p = new Option("p","pwd", true, "密码，如果密码前有!符，请将密码前后加引号'");
//		options.addOption(p);
//		Option  db = new Option("db","database", true, "mongodb的库名，有权限访问的");
//		options.addOption(db);
		Option debug = new Option("d","debug", true, "调试命令行程序集时使用，需指定以下jar包所在目录\r\n"+fileName);
		options.addOption(debug);

		// GnuParser
		// BasicParser
		// PosixParser
		GnuParser parser = new GnuParser();
		CommandLine line = parser.parse(options, args);
		if (line.hasOption("m")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("gateway", options);
			return;
		}
		
		// 取属性的方式line.getOptionProperties("T").get("boss")
		// System.out.println(line.getOptionProperties("T").get("boss"));
//		if(StringUtil.isEmpty(line.getOptionValue("h")))
//			throw new ParseException("参数-h是host为必需，但为空");
		
		String usr = System.getProperty("user.dir");
		File f = new File(usr);
		File[] arr = f.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				if (name.startsWith(fileName)) {
					return true;
				}
				return false;
			}
		});
		if (arr.length < 1 && !line.hasOption("debug")) {
			throw new IOException(fileName + " 程序集不存在.");
		}
		if (line.hasOption("debug")) {
			File[] da = new File(line.getOptionValue("debug")).listFiles(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					if (name.startsWith(fileName)) {
						return true;
					}
					return false;
				}
			});
			if (da.length < 0)
				throw new IOException("调试时不存在指定的必要jar包" + fileName);
			f = da[0];
		} else {
			f = arr[0];
		}

		IAssembly assembly = Assembly.loadAssembly(f.toString());
		assembly.start();
		Object main = assembly.workbin().part("gatewayEntrypoint");
		IAdaptable a = (IAdaptable) main;
		IActuator act = a.getAdapter(IActuator.class);
		act.exactCommand("setHomeDir", new Class<?>[]{String.class}, f.getParent());
		act.exeCommand("main", line);

	}

}
```
  2.建立build.gradle,将下面考进去
  ```groovy
apply plugin:'application'
mainClassName = "cj.netos.microapp.main.Main"
sourceSets { 
	 main { 
	 	java{
	 		srcDir "$projectDir/src"
	 	}
	 	resources{
		 	srcDir "$projectDir/src"
		 }
	 } 
 	}
 		sourceCompatibility = 1.8
    targetCompatibility = 1.8
 	tasks.withType(JavaCompile) {  
        options.encoding = "UTF-8"  
    } 
 repositories { 
 	mavenCentral();
 }

dependencies {
	compile group: 'com.squareup.okhttp3', name: 'okhttp', version: '4.2.1'
    compile group: 'log4j', name: 'log4j', version: '1.2.17'
}
```
  注意：引用log4j,okhttp一般则可以了，如果提示缺包则再添加引用(注：这些包在使用linux和windows脚本启动时并不缺，仅用在调试)
  3. 在eclipse或idea中先以java application选项运行一下main类，之后找到java application生成的项，修改其配置项，为其添加program arguments参数：
  -debug /Users/caroceanjofers/studio/github/cj.studio.gateway2/cmdtools/gateway
  -deubg参数后面是网关所在的路径
  4. 接下来就可以正常通过eclipse或idea的debug或run按钮启动网关了
- 直接使用idea的Jar Application
  1。直接在idea的run/debug 配置界面中新建run/debug项
  2。在Path To Jar文本框中输入网关主jar地址，如：/Users/caroceanjofers/studio/github/cj.netos.gbera/cj.netos.gbera/cmdtools/gateway/gateway-2.3.8.jar
  3。在Program arguments文本框中输入网关主目录所在地址：-debug /Users/caroceanjofers/studio/github/cj.netos.gbera/cj.netos.gbera/cmdtools/gateway
- 我们看到使用idea更方便配置网关应用，所以推荐使用idea开发工具。但idea不能像eclipse在调试中修改java代码立即生效，它需要每次重启，就是第一种方法在命名用idea时也一样。


* orm.mongodb配置
其中isTrustConnect为true表示采用确信连接，false采用账户密码连接（需mongodb支持）。默认为确信连接
```json
plugins: [
			{
				name:"mongodb",
				class:"cj.studio.orm.mongodb.MongoDBPlugin",
				parameters:{
					remotes:"[
					'localhost:27017'
				    ]",
					isTrustConnect: "false",
					database: "admin",
					user: 'superadmin',
					password: '!jofers0227'
				}
			}
		],
```