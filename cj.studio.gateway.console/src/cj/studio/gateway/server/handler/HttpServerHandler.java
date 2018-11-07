package cj.studio.gateway.server.handler;

import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IChipInfo;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.logging.ILogging;
import cj.studio.gateway.ICluster;
import cj.studio.gateway.IDestinationLoader;
import cj.studio.gateway.IGatewaySocketContainer;
import cj.studio.gateway.IJunctionTable;
import cj.studio.gateway.conf.ServerInfo;
import cj.studio.gateway.junction.ForwardJunction;
import cj.studio.gateway.junction.Junction;
import cj.studio.gateway.server.util.DefaultHttpMineTypeFactory;
import cj.studio.gateway.server.util.GetwayDestHelper;
import cj.studio.gateway.socket.Destination;
import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import cj.studio.gateway.socket.pipeline.IInputPipelineBuilder;
import cj.studio.gateway.socket.pipeline.InputPipelineCollection;
import cj.studio.gateway.socket.util.SocketContants;
import cj.studio.gateway.socket.util.SocketName;
import cj.studio.gateway.socket.ws.WebsocketServerChannelGatewaySocket;
import cj.ultimate.util.FileHelper;
import cj.ultimate.util.StringUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;

public class HttpServerHandler extends SimpleChannelInboundHandler<Object> implements SocketContants {
	static String Websocket_Channel = "Websocket-channel";
	private WebSocketServerHandshaker handshaker;
	IServiceProvider parent;
	public static ILogging logger;
	IGatewaySocketContainer sockets;
	private IJunctionTable junctions;
	InputPipelineCollection pipelines;
	private ServerInfo info;

	public HttpServerHandler(IServiceProvider parent) {
		this.parent = parent;
		logger = CJSystem.logging();
		sockets = (IGatewaySocketContainer) parent.getService("$.container.socket");
		junctions = (IJunctionTable) parent.getService("$.junctions");
		this.pipelines = new InputPipelineCollection();
		this.info = (ServerInfo) parent.getService("$.server.info");
	}

	@Override
	protected void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof FullHttpRequest) {
			handleHttpRequest(ctx, (FullHttpRequest) msg);
		} else if (msg instanceof WebSocketFrame) {
			handleWebSocketFrame(ctx, (WebSocketFrame) msg);
		}
	}

	private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
		if (!req.getDecoderResult().isSuccess()) {
			doResponse(ctx, req, new DefaultFullHttpResponse(req.getProtocolVersion(), BAD_REQUEST));
			return;
		}

		// deny PUT TRACE methods.
		if (req.getMethod() == HttpMethod.PUT || req.getMethod() == HttpMethod.TRACE) {
			doResponse(ctx, req, new DefaultFullHttpResponse(req.getProtocolVersion(), FORBIDDEN));
			return;
		}
		if ("/favicon.ico".equals(req.getUri())) {
			FullHttpResponse res = new DefaultFullHttpResponse(req.getProtocolVersion(), HttpResponseStatus.OK);
			IChipInfo cinfo = (IChipInfo) parent.getService("$.chipinfo");
			InputStream gatewayLogo = cinfo.getIconStream();
			byte[] buf = FileHelper.readFully(gatewayLogo);
			res.content().writeBytes(buf);
			doResponse(ctx, req, res);
			return;
		}
		// Send the demo page and favicon.ico
		if (!isWebSocketReq(req)) {
			FullHttpResponse res = new DefaultFullHttpResponse(req.getProtocolVersion(), HttpResponseStatus.OK);
			flowHttpRequest(ctx, req, res);
			doResponse(ctx, req, res);
			return;
		}

		// Handshake
		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req),
				null, false, 10485760);
		handshaker = wsFactory.newHandshaker(req);
		if (handshaker == null) {
			WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());
		} else {
			handshaker.handshake(ctx.channel(), req);
			websocketActive(ctx, req);
		}
	}

	protected void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) throws CircuitException {
		// Check for closing frame
		if (frame instanceof CloseWebSocketFrame) {
			handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
			return;
		}
		if (frame instanceof PingWebSocketFrame) {
			ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
			return;
		}

		String name = SocketName.name(ctx.channel().id(), info.getName());
		IInputPipeline inputPipeline = pipelines.get(name);
		if (inputPipeline == null) {
			throw new CircuitException("404", "目标管道不存在" + name);
		}
		inputPipeline.headFlow(frame, null);
	}

	protected void websocketActive(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
		WebsocketServerChannelGatewaySocket wsSocket = new WebsocketServerChannelGatewaySocket(parent, ctx.channel());
		sockets.add(wsSocket);// ws是双向通讯端子，故需加入
		// 以下生成目标管道
		String uri = req.getUri();
		try {
			uri = URLDecoder.decode(uri, "utf-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		String gatewayDestInHeader = req.headers().get(__frame_gatewayDest);
		String gatewayDest = GetwayDestHelper.getGatewayDestForHttpRequest(uri, gatewayDestInHeader, getClass());
		if (StringUtil.isEmpty(gatewayDest) || gatewayDest.endsWith("://")) {
			throw new CircuitException("404", "缺少路由目标，请求侦被丢掉：" + uri);
		}

		IGatewaySocket socket = this.sockets.find(gatewayDest);
		if (socket == null) {
			ICluster cluster = (ICluster) parent.getService("$.cluster");
			Destination destination = cluster.getDestination(gatewayDest);
			if (destination == null) {
				throw new CircuitException("404", "簇中缺少目标:" + gatewayDest);
			}
			IDestinationLoader loader = (IDestinationLoader) parent.getService("$.dloader");
			socket = loader.load(destination);
			sockets.add(socket);
		}

		IInputPipelineBuilder builder = (IInputPipelineBuilder) socket.getService("$.pipeline.input.builder");
		String name = SocketName.name(ctx.channel().id(), info.getName());
		IInputPipeline inputPipeline = builder.name(name).prop(__pipeline_fromProtocol, "ws")
				.prop(__pipeline_fromWho, info.getName()).createPipeline();
		pipelines.add(name, inputPipeline);

		ForwardJunction junction = new ForwardJunction(name);
		junction.parse(inputPipeline, ctx.channel(), socket);
		this.junctions.add(junction);

		FullHttpResponse res = new DefaultFullHttpResponse(req.getProtocolVersion(), HttpResponseStatus.OK);
		inputPipeline.headOnActive(name, req, res);// 通知管道激活
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		String name = SocketName.name(ctx.channel().id(), info.getName());

//		websocketInactive(ctx, name);//由于ws的channel是在http上升级的channel，故channelInactive一样执行。如果特意要释放ws channel的资源，可以根据ch.pipeline的handlers来判断是否ws channel，因为ws ch包含websocket handlers

		Junction junction = junctions.findInForwards(name);
		if (junction != null) {
			this.junctions.remove(junction);
		}

		if (sockets.contains(name)) {
			sockets.remove(name);// 不论是ws还是http增加的，在此安全移除
		}

		IInputPipeline input = pipelines.get(name);
		if (input != null) {
			input.headOnInactive(name);
			pipelines.remove(name);
		}
		super.channelInactive(ctx);
	}

	protected void flowHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res)
			throws Exception {
		String uri = req.getUri();
		try {
			uri = URLDecoder.decode(uri, "utf-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		String gatewayDestInHeader = req.headers().get(__frame_gatewayDest);
		String gatewayDest = GetwayDestHelper.getGatewayDestForHttpRequest(uri, gatewayDestInHeader, getClass());
		if (StringUtil.isEmpty(gatewayDest) || gatewayDest.endsWith("://")) {
			throw new CircuitException("404", "缺少路由目标，请求侦被丢掉：" + uri);
		}

		String name = SocketName.name(ctx.channel().id(), info.getName());
		IInputPipeline inputPipeline = pipelines.get(name);
		// 检查目标管道是否存在
		if (inputPipeline != null) {
			inputPipeline.headFlow(req, res);
			return;
		}
		// 目标管道不存在，以下生成目标管道
		// 检查并生成目标管道
		IGatewaySocket socket = this.sockets.find(gatewayDest);
		if (socket == null) {
			ICluster cluster = (ICluster) parent.getService("$.cluster");
			Destination destination = cluster.getDestination(gatewayDest);
			if (destination == null) {
				throw new CircuitException("404", "簇中缺少目标:" + gatewayDest);
			}
			IDestinationLoader loader = (IDestinationLoader) parent.getService("$.dloader");
			socket = loader.load(destination);
			sockets.add(socket);
		}

		IInputPipelineBuilder builder = (IInputPipelineBuilder) socket.getService("$.pipeline.input.builder");
		inputPipeline = builder.name(name).prop(__pipeline_fromProtocol, "http")
				.prop(__pipeline_fromWho, info.getName()).createPipeline();
		pipelines.add(name, inputPipeline);

		ForwardJunction junction = new ForwardJunction(name);
		junction.parse(inputPipeline, ctx.channel(), socket);
		this.junctions.add(junction);

		inputPipeline.headOnActive(name, req, res);// 通知管道激活

		inputPipeline.headFlow(req, res);// 将当前请求发过去
	}

	protected void doResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
		HttpHeaders headers = res.headers();
		if (res.getStatus().code() != 200) {
			setContentLength(res, res.content().readableBytes());
		} else {
			if (!headers.contains(HttpHeaders.Names.CONTENT_LENGTH.toString())) {
				setContentLength(res, res.content().readableBytes());
			}
		}
		String ctypeKey = HttpHeaders.Names.CONTENT_TYPE.toString();
		if (!headers.contains(ctypeKey)) {
			if (req.headers().contains(ctypeKey)) {
				headers.add(ctypeKey, req.headers().get(ctypeKey));
			} else {
				if (!headers.contains(ctypeKey)) {
					String extName = req.getUri();
					int pos = extName.lastIndexOf(".");
					extName = extName.substring(pos + 1, extName.length());
					if (DefaultHttpMineTypeFactory.containsMime(extName)) {
						headers.add(ctypeKey, DefaultHttpMineTypeFactory.mime(extName));
					} else {
						headers.add(ctypeKey, "text/html; charset=utf-8");
					}
				}
			}
		}
		// Send the response and close the connection if necessary.
		ChannelFuture f = ctx.channel().writeAndFlush(res);
		if (!isKeepAlive(req) || res.getStatus().code() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}

	}

	protected boolean isWebSocketReq(HttpRequest req) {
		if (req.headers().get("Connection") == null)
			return false;
		return ((req.headers().get("Connection").contains("Upgrade"))
				&& "websocket".equalsIgnoreCase(req.headers().get("Upgrade")));
	}

	protected String getWebSocketLocation(FullHttpRequest req) {
		String path = info.getProps().get("Websocket-Path");
		if (StringUtil.isEmpty(path)) {
			path = "/websocket";
		} else {
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
		}

		String uri = req.getUri();
		int pos = uri.lastIndexOf("?");
		if (pos > 0) {
			uri = uri.substring(0, pos);
		}
		if ("/".equals(uri)) {
			return "ws://" + req.headers().get(HOST) + path;
		}
		while (uri.startsWith("/")) {
			uri = uri.substring(1, uri.length());
		}
		pos = uri.indexOf("/");
		if (pos > 0) {
			uri = uri.substring(0, pos);
		}
		return "ws://" + req.headers().get(HOST) + "/" + uri + path;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (cause instanceof CircuitException) {
			CircuitException c = (CircuitException) cause;
			if ("404".equals(c.getStatus())) {
				logger.error(this.getClass(), cause.getMessage());
			} else {
				logger.error(this.getClass(), cause);
			}
		} else {
			if (cause instanceof IOException) {
				logger.error(this.getClass(), cause.getMessage());
			} else {
				logger.error(this.getClass(), cause);
			}
		}

		if (isWebsocketChannel(ctx)) {
			errorWebsocketCaught(ctx, cause);
			return;
		}
		errorHttpCaught(ctx, cause);
	}

	private boolean isWebsocketChannel(ChannelHandlerContext ctx) {
		ChannelPipeline pipeline = ctx.pipeline();
		List<String> names = pipeline.names();
		for (String name : names) {
//			ChannelHandler handler=pipeline.get(name);
			if ("wsdecoder".equals(name) || "wsencoder".equals(name)) {
				return true;
			}
//			if(handler instanceof ByteToMessageDecoder) {
//				return true;
//			}
		}
		return false;
	}

	private void errorHttpCaught(ChannelHandlerContext ctx, Throwable cause) {
		Throwable e = null;
		e = CircuitException.search(cause);
		if (e == null) {
			e = cause;
		}
		if (e instanceof CircuitException) {
			FullHttpResponse res = null;
			String err = "";
			CircuitException c = ((CircuitException) e);
			if ("404".equals(c.getStatus())) {
				HttpResponseStatus status = new HttpResponseStatus(Integer.parseInt(c.getStatus()),
						HttpResponseStatus.NOT_FOUND.reasonPhrase());
				res = new DefaultFullHttpResponse(HTTP_1_1, status);
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				cause.printStackTrace(pw);
				err = sw.toString();
			} else {
				String msg = e.getMessage().replace("\r", "").replace("\n", "<br/>");// netty的HttpResponseStatus构造对\r\n作了错误异常
				HttpResponseStatus status = new HttpResponseStatus(Integer.parseInt(c.getStatus()), msg);
				res = new DefaultFullHttpResponse(HTTP_1_1, status);
				err = ((CircuitException) e).messageCause();
			}
			// err = err.replaceAll("\r\n", "<br/>");
			res.content().writeBytes(err.getBytes());
			// 注意：对于一些http状态号，浏览器不解析，比如204,浏览器得到此状态码后什么也不做。因此要想将错误呈现到界面上，需依据http协议的状态码定义。
			DefaultFullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
			HttpHeaders.setKeepAlive(req, false);
			doResponse(ctx, req, res);
		} else {
			String error = e.getMessage();
			if (!StringUtil.isEmpty(error)) {
				error.replace("\r", "").replace("\n", "<br/>");
			}else {
				error="";
			}
			HttpResponseStatus status = new HttpResponseStatus(503, error);
			FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, status);
			DefaultFullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
			HttpHeaders.setKeepAlive(req, false);
			doResponse(ctx, req, res);
		}
		ctx.close();
	}

	private void errorWebsocketCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		TextWebSocketFrame frame = new TextWebSocketFrame();
		Frame f = new Frame("error / gateway/1.0");

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		cause.printStackTrace(pw);
		CircuitException cir = CircuitException.search(cause);
		if (cir != null) {
			f.head("status", cir.getStatus());
		} else {
			f.head("status", "503");
		}
		f.head("message", cause.getMessage().replace("\r", "").replace("\n", ""));
		boolean is_401 = "401".equals(f.head("status"));
		if (is_401) {
			f.url("/handshake");// error /hanshake gateway/1.0
		}
		f.content().writeBytes(sw.toString().getBytes("utf-8"));
		frame.content().writeBytes(f.toByteBuf());
		ctx.writeAndFlush(frame);
		sw.close();
		if (is_401) {
			// 握手失败，关闭连接
			ctx.writeAndFlush(new CloseWebSocketFrame());
			ctx.channel().close();
		}
	}
}
