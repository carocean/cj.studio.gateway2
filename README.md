## Gateway2.0

High-Performance Distributed Microservice Container and Connectivity Platform

Gateway 2.0 is an advanced distributed microservice container and connectivity platform, built on the ECM development framework and leveraging the Netty communication layer with NIO (Non-blocking I/O) technology. Supporting protocols such as HTTP, WebSocket, TCP, and UDT, it serves as a foundational infrastructure for modern distributed architectures and microservice ecosystems.

Key Features and Strategic Advantages

	1.	Exceptional Communication Efficiency
	•	Utilizes NIO technology to achieve low latency and high throughput, ideal for real-time, high-volume distributed data streams.
	•	Supports multiple communication protocols, seamlessly integrating with web, real-time, and high-speed data transfer scenarios.
	2.	Modular Scalability
	•	Built on the ECM framework, it enables modular development and dynamic deployment, allowing rapid scalability to meet evolving business needs.
	•	Its distributed architecture supports the construction of multi-layer systems, enhancing fault tolerance and operational efficiency.
	3.	Java-Based JavaScript Services (JSS Services)
	•	Gateway 2.0 introduces support for Java-based JavaScript services (JSS services), a core innovation within the ECM ecosystem.
	•	JSS services integrate seamlessly with traditional Java services, enabling developers to harness the flexibility of JavaScript while leveraging Java’s robust performance and strong typing.
	•	This capability enhances development efficiency and expands the technological options for service creation, especially in complex distributed systems.
	4.	Cloud-Native Readiness
	•	Designed with cloud-native principles, it integrates seamlessly with containerization tools like Docker and orchestration platforms like Kubernetes, ensuring compatibility with modern infrastructure.
	•	Provides standardized tools to build resilient microservice architectures, reducing complexity and accelerating development.
	5.	Enhanced Microservice Management
	•	Integrates microservice registration, discovery, routing, and circuit-breaker functionalities, with any gateway capable of serving as a microservice center.
	•	Lightweight implementation ensures rapid deployment and dynamic scaling, delivering agility and responsiveness.
	6.	Developer-Friendly Design
	•	Offers unbuffered data handling, eliminating traditional file size limitations, giving developers precise control over data processing, and boosting performance.
	•	Comes equipped with rich debugging tools and configuration options, simplifying development workflows and accelerating time-to-market.
	7.	Innovative Subscription and Messaging Mechanism
	•	Features a subscription-based broadcast mechanism, enabling real-time synchronization and message distribution across gateway nodes.
	•	Supports customizable topics, making it ideal for managing complex communication flows, akin to tools like Memcache and Zookeeper.

Why Gateway 2.0 Leads the Future of Distributed Systems

Gateway 2.0 sets a new benchmark for distributed microservice platforms. Its modular design, high-performance communication, JavaScript service support, and cloud-native compatibility provide robust foundations for scalable, fault-tolerant systems. As a unified solution for microservice orchestration and connectivity, Gateway 2.0 empowers enterprises to achieve agility, efficiency, and technological innovation in the era of distributed computing.



# Example Projects

You can refer to two other projects on my GitHub: cj.studio.gateway.examples.frontend and cj.studio.gateway.examples.backend.
The Multipart requests have been parsed in-memory, but to avoid issues like the file upload size limit and disk buffering encountered in web containers like Tomcat or Jetty, this method has poor performance and flexibility. Gateway2.0 will open the data interface to developers, allowing them to decide how to handle it, with no buffering in between, thus enabling unlimited file upload capabilities.

Upcoming features to be implemented include:

	•	HTTP Server: Used for forwarding HTTP chunks in multi-tier gateways without any processing in between, similar to the functionality of NGINX and Apache.
	•	Subscription-based Broadcast Application: Multiple gateways aggregate to a target gateway’s topic. When the topic receives a message, it distributes it to all subscribed nodes. The topic can be created by operation and maintenance personnel. This feature is similar to Memcache and Zookeeper and is used for synchronizing gateway nodes.
	•	Heartbeat Mechanism for Various Network Protocols.
	•	Microservice Definition and Client Call Mechanism, including registration with any gateway (any gateway can serve as a microservice center). Since registration (reporting microservice information), routing, discovery, and circuit-breaking reconnection are fundamental gateway functions, the microservice mechanism can be implemented within two working days.

Gateway can be used for developing website projects or microservice projects, referred to as Gateway Applications. When used as a distributed platform, it supports microservice registration, discovery, routing, circuit-breaking retries, and other functionalities. It can evolve into an N-layer distributed architecture, with layers created by operation and maintenance personnel.

For small to medium-sized internet companies, Gateway is a game-changer. These companies often lack the resources or capital to develop their own distributed system infrastructure. The one-stop solution provided by Gateway can significantly reduce development costs while handling business growth. With the rising popularity of microservice architectures and Docker containers in recent years, Gateway will likely play a key role in the future “cloud”-based software development styles. It provides a standardized, full-stack technical solution amidst the plethora of distributed solutions, which could be as significant as the introduction of the Servlet specification in advancing server-side software system technology.

Additional Products from cj.studio:

	•	ECM Development Kit: Combines the functionality of Spring, OSGi, and Node.js. Supports module-based development and deployment with hot swapping.
	•	NET Development Kit: Supports web development and can fully develop web applications using JavaScript. Its syntax is similar to popular Node.js, and it includes a Netty-based NIO framework and a custom-developed NIO framework called RIO (Reactive I/O), which combines NIO advantages with synchronous waiting for responses.
	•	Plus Development Kit: Enhances connection capabilities, including plugin-based web applications (like Eclipse’s plugin architecture), support for remote services like WebService, and cloud computing chip development.
	•	NetDisk: An enhanced development kit based on MongoDB. It implements various file system functions, supporting random file access and structured data access, multidimensional features, user quotas, SQL-like syntax support, and object mapping.
	•	Neuron Tool: Similar to service containers like Tomcat/Jetty, it is more focused on connecting backward and is a tool for building large distributed neural networks. Its goal is to create neural network clusters.
	•	MDisk CLI Tool: A command-line tool for NetDisk, facilitating MongoDB development, testing, and operation management. It’s simple to use and connects directly to MongoDB as a disk storage database without affecting the original MongoDB database.
	•	CJNet: A debugging tool for applications in Neuron and Netsite, essential for developing and debugging net-related products in the CJ Studio product suite.
	•	Netsite: A command-line tool for service containers like Tomcat/Jetty. Unlike Neuron, it can only be deployed on the end node of a neural network and cannot serve as an intermediate node. It can deploy hundreds or even thousands of applications, though this is less common on Neuron nodes. This tool is currently no longer being updated.

Compatibility with NGINX

	•	When using the default NGINX configuration, you may encounter the error: upstream prematurely closed connection while reading upstream.
	•	Gateway does not support the HTTP/1.0 protocol. Requests using HTTP/1.0 will be rejected. NGINX, by default, uses HTTP/1.0 (it rewrites requests to HTTP/1.0 regardless of whether the browser sends HTTP/1.1). Therefore, it is necessary to modify the configuration to support HTTP/1.1.

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

## Debugging Methods for Gateway Applications

There are two debugging methods: one is to create a main project, which is suitable for Eclipse and IntelliJ IDEA development tools; the other is to directly use IntelliJ IDEA’s Jar Application, which is only suitable for IntelliJ IDEA.

	•	Debugging by Creating a Main Project
	1.	Create a project and copy the following code into it.
Copy the code:
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
  2.Create a build.gradle file and copy the following into it:
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
  Note: Referencing log4j and okhttp should generally be sufficient. If there are missing packages, add the necessary dependencies (Note: these packages are not required when using Linux and Windows scripts to start, only during debugging).

	3.	In Eclipse or IntelliJ IDEA, first run the main class using the “Java Application” option. Then, find the Java application entry and modify its configuration, adding the following program arguments:
	•	-debug /Users/caroceanjofers/studio/github/cj.studio.gateway2/cmdtools/gateway
The path after the -debug parameter is the location of the gateway.
	4.	After that, you can start the gateway normally using the debug or run buttons in Eclipse or IntelliJ IDEA.

	•	Using IntelliJ IDEA’s Jar Application
	1.	In the IntelliJ IDEA run/debug configuration window, create a new run/debug configuration.
	2.	In the “Path to Jar” field, enter the path to the main gateway JAR file, e.g., /Users/caroceanjofers/studio/github/cj.netos.gbera/cj.netos.gbera/cmdtools/gateway/gateway-2.3.8.jar.
	3.	In the “Program arguments” field, enter the main gateway directory path: -debug /Users/caroceanjofers/studio/github/cj.netos.gbera/cj.netos.gbera/cmdtools/gateway.

We find that using IntelliJ IDEA is more convenient for configuring the gateway application, so we recommend using IntelliJ IDEA as the development tool. However, IntelliJ IDEA cannot apply changes to Java code immediately during debugging like Eclipse can. It requires a restart each time, which is the same for the first method when using IDEA.

* ORM.MongoDB Configuration
The isTrustConnect parameter, when set to true, indicates a trusted connection. When set to false, it uses account-password authentication (requiring MongoDB support). The default is a trusted connection.
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
