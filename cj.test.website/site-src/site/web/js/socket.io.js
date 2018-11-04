if (typeof $ == 'undefined'){
	$ = {};
}
$.parameter = function (name) {
    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
    var r = window.location.search.substr(1).match(reg);
    if (r != null) return unescape(r[2]); return null;
}
$.ws = {
	toFrame : function(frameRaw) {
		// debugger;
		var up = 0;
		var down = 0;
		var field = 0;// 0=heads;1=params;2=content
		var frame = {
			heads : {},
			params : {}
		};
		while (down < frameRaw.length) {
			if(field<2){//参见frame.java
				if (frameRaw[up] == '\r'
						&& (up + 1 < frameRaw.length && frameRaw[up + 1] == '\n')) {// 跳域
					field++;
					up += 2;
					down += 2;
					continue;
				}
			}else {
				down = frameRaw.length;
				var content = frameRaw.substr(up, down - up);
				frame.content = content;
				break;
			}
			if (frameRaw[down] == '\r'
					&& (down + 1 < frameRaw.length && frameRaw[down + 1] == '\n')) {// 跳行
				var b = frameRaw.substr(up, down - up);
				switch (field) {
				case 0:
					var kv = b;
					var at = kv.indexOf("=");
					var k = kv.substr(0, at);
					var v = kv.substr(at + 1, kv.length);
					frame.heads[k] = v;
					break;
				case 1:
					kv = b;
					at = kv.indexOf("=");
					k = kv.substr(0, at);
					v = kv.substr(at + 1, kv.length);
					frame.params[k] = v;
					break;
				}
				down += 2;
				up = down;
				continue;
			}
			down++;
		}
		return frame;
	},
	open : function(wsurl, onmessage, onopen, onclose,onerror) {
		var doReceive = function(e) {
			var frame = $.ws.toFrame(e.data);
			var status=parseInt(frame.status);
			if(status>=300){
				alert(frame.message);
				return;
			}
			if('frame/bin'!=frame.heads['Content-Type']&&'frame/json'!=frame.heads['Content-Type']){
				if (typeof onmessage != 'undefined' && onmessage != null) {
					onmessage(frame);
				}
				return;
			}
			var len=parseInt(frame.heads["Content-Length"]);
			if(len<1){
				return;
			}
			frame=$.ws.toFrame(frame.content);
			if (typeof onmessage != 'undefined' && onmessage != null) {
				onmessage(frame);
			}
		}
		
		var websocket = {
			ws : wsurl,
			init:function(){
				var socket;
				if (!window.WebSocket) {
					window.WebSocket = window.MozWebSocket;
				}
				if (window.WebSocket) {
					socket = new WebSocket(wsurl);
					socket.onmessage = doReceive;
					socket.onopen = onopen;
					socket.onerror=onerror;
					socket.onclose = onclose;
				} else {
					alert("Your browser does not support Web Socket.\r\n please download newrest browser version or download chrome or firbox .");
					return;
				}
				this.socket=socket;
			},
			send : function(frame) {
				if (!window.WebSocket) {//将来添加comet技术，以模拟websocket的api
					return;
				}
				var rule=/^protocol\s*=\s*(\S+)$/mg;
				var groups=rule.exec(frame);
				if(groups.length<1){
					alert('不是侦格式');
					return;
				}
				var socket = this.socket;
				if (socket.readyState == WebSocket.OPEN) {
					socket.send(frame);
				} else {
					alert("The socket is not open.");
				}
			},
			close : function() {
				socket.close();
			}
		};
		websocket.init();
		return websocket;
	}
}
