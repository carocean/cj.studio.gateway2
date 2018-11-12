/*
 * 2016.0829
 * 作者：赵向彬
 * 说明：services是声明本jss要引用的服务
 * <![jss:{
		scope:'runtime',
		extends:'cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView',
		isStronglyJss:true,
		filter:{
	 	} 	
	},
	shit:{
	 		name:"test..."
	},
 	services:{
 		tcp:'/tcp'
 	}
 ]>
 <![desc:{
	ttt:'2323',
	obj:{
		name:'09skdkdk'
		}
* }]>
 */

var String = Java.type('java.lang.String');
var WebUtil = Java.type('cj.studio.ecm.net.web.WebUtil');
var BigInteger=Java.type('java.math.BigInteger');
var CircuitException=Java.type('cj.studio.ecm.graph.CircuitException');
var SimpleDateFormat = Java.type('java.text.SimpleDateFormat');
var Date = Java.type('java.util.Date');
var Long = Java.type('java.lang.Long');



exports.flow = function(req,resp,ctx) {
	print('---------------');
	print('module_name:' + imports.module_name);
	print('module_home:' + imports.module_home);
	print('module_ext:' + imports.module_extName);
	print('module_pack:' + imports.module_package);
	print('module_unzip:' + imports.module_unzip);
	print('module_type:' + imports.module_type);
	print('head jss scope:'+imports.head.jss.scope);
	print('head shit name:'+imports.head.shit.name);
	print('head desc ttt:'+imports.head.desc.ttt);
	print('location:' + imports.locaction);
	print('source:' + imports.source);
	print('selectKey1:' + imports.selectKey1);
	print('selectKey2:' + imports.selectKey2);
	print('this is jss chip site ' + chip.site());
	var info = chip.info();
	print(info.id);
	print(info.name);
	print(info.version);
	print(info.getProperty('home.dir'));
	print('--selectKey1:' + imports.selectKey1);
	print('--selectKey2:' + imports.selectKey2);
	print('--- services is :'+imports.head.services.tcp);
	print('hello world jss');
	
	print('-----------------end.')
	
	var doc = ctx.html("/index.html", "utf-8");
	
	resp.content().writeBytes(doc.toString().getBytes());
}
