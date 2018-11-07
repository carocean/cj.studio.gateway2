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
 	services:{
 		
 	}
 ]>
 */

var String = Java.type('java.lang.String');
var WebUtil = Java.type('cj.studio.ecm.net.web.WebUtil');
var BigInteger=Java.type('java.math.BigInteger');
var CircuitException=Java.type('cj.studio.ecm.graph.CircuitException');
var SimpleDateFormat = Java.type('java.text.SimpleDateFormat');
var Date = Java.type('java.util.Date');
var Long = Java.type('java.lang.Long');

exports.flow = function(req,resp,ctx) {
	var doc = ctx.html("/index.html", "utf-8");
	
	resp.content().writeBytes(doc.toString().getBytes());
}
