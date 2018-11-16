$(document).ready(function(){
	var euser=$('a.euser');
	//websocket登录到远程
	var wsurl=euser.attr('wsurl');//该地址可从程序集的属性文件中读取,并由服务器附加到页面元素上，由此而取到。
	var ws=$.ws.open(wsurl,function(frame){
		//onmessage
		var url=frame.heads.url;
		var protocol=frame.heads.protocol;
		var cmd=frame.heads.command;
		console.log(cmd+' '+ url+ ' '+protocol+'\r\n'+frame.content);
		$('.my2').val(frame.content);
	},function(f){
		euser.html('已连接');
		euser.attr('status','connected');
		//onopen
		//此处实现websocket的网关认证
//		var domain='ws://wigo.facility.com';//域名
//		var token='Iukyy3LweC/GY09044poPwBFOaRaTEFynl4SJFoALYHwQ4sznHiKlL+wnzAmPWzK+EyjRwn8F7jSo2k/aBplfTQQ5jL0nlMwQjWy4ku+NQ7i0CQjftVV3+bzIuZKiTplUrKvneRc3ElXYyYCrYj9x9lg2kaKtwsvPliCfYI9v0Q=';//网关对此域名的令牌
		var domain='ws://'+euser.attr('user')+'.com';
		var token=euser.attr('token');
		//var frame ="command=handshake\r\nprotocol=gateway/1.0\r\nurl=/\r\nGateway-Inputer="+domain+"\r\nGateway-Token="+token+"\r\n\r\n\r\n";
		var frame ="command=get\r\nprotocol=gateway/1.0\r\nurl=/test/websocket.html\r\nGateway-Token="+token+"\r\n\r\n\r\n";
		ws.send(frame);
	},function(frame){
		//onclose
		euser.html('已断开');
		euser.attr('status','disconnected');
	},function(frame){
		//onerror
		console.log(frame);
		//alert(frame.head('status')+' '+frame.head('message'));
	})	;
	
});