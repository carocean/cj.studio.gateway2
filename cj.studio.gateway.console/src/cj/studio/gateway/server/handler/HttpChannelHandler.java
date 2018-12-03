package cj.studio.gateway.server.handler;

import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
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
import java.util.Map.Entry;
import java.util.Set;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.IChipInfo;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.logging.ILogging;
import cj.studio.gateway.IGatewaySocketContainer;
import cj.studio.gateway.IJunctionTable;
import cj.studio.gateway.conf.ServerInfo;
import cj.studio.gateway.http.args.HttpContentArgs;
import cj.studio.gateway.http.args.HttpRequestArgs;
import cj.studio.gateway.junction.ForwardJunction;
import cj.studio.gateway.junction.Junction;
import cj.studio.gateway.server.util.DefaultHttpMineTypeFactory;
import cj.studio.gateway.server.util.GetwayDestHelper;
import cj.studio.gateway.socket.IChunkVisitor;
import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import cj.studio.gateway.socket.pipeline.IInputPipelineBuilder;
import cj.studio.gateway.socket.pipeline.InputPipelineCollection;
import cj.studio.gateway.socket.serverchannel.ws.WebsocketServerChannelGatewaySocket;
import cj.studio.gateway.socket.util.SocketContants;
import cj.studio.gateway.socket.util.SocketName;
import cj.studio.gateway.socket.visitor.IHttpFormChunkDecoder;
import cj.ultimate.util.FileHelper;
import cj.ultimate.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;

public class HttpChannelHandler extends SimpleChannelInboundHandler<Object> implements SocketContants {
	static String Websocket_Channel = "Websocket-channel";
	private WebSocketServerHandshaker handshaker;
	IServiceProvider parent;
	public static ILogging logger;
	IGatewaySocketContainer sockets;
	private IJunctionTable junctions;
	InputPipelineCollection pipelines;
	private ServerInfo info;
	boolean keepLive;
	private IHttpFormChunkDecoder decoder;
	private IChunkVisitor visitor;
	private String currentUsedGatewayDestForHttp;

	public HttpChannelHandler(IServiceProvider parent) {
		this.parent = parent;
		logger = CJSystem.logging();
		sockets = (IGatewaySocketContainer) parent.getService("$.container.socket");
		junctions = (IJunctionTable) parent.getService("$.junctions");
		this.pipelines = new InputPipelineCollection();
		this.info = (ServerInfo) parent.getService("$.server.info");
	}

	@Override
	protected void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof HttpRequest) {
			HttpRequest req = (HttpRequest) msg;
			this.keepLive = req.headers().contains(CONNECTION, HttpHeaders.Values.CLOSE, true)
					|| req.getProtocolVersion().equals(HttpVersion.HTTP_1_0)
							&& !req.headers().contains(CONNECTION, HttpHeaders.Values.KEEP_ALIVE, true);
			handleHttpRequest(ctx, req);
		} else if (msg instanceof HttpContent) {
			IInputPipeline inputPipeline = pipelines.get(currentUsedGatewayDestForHttp);
			if (inputPipeline != null) {
				HttpContentArgs args = new HttpContentArgs(ctx, decoder, visitor, keepLive);
				inputPipeline.headFlow(msg, args);
				if (args.isDisposed()) {
					this.decoder = null;
					this.visitor = null;
				}
				return;
			}
		} else if (msg instanceof WebSocketFrame) {
			flowWebSocketFrame(ctx, (WebSocketFrame) msg);
		} else {
			throw new CircuitException("801", "不支持的消息类型：" + msg.getClass());
		}
	}

	private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest req) throws Exception {
		if (!req.getDecoderResult().isSuccess()) {
			DefaultFullHttpResponse res = new DefaultFullHttpResponse(req.getProtocolVersion(), BAD_REQUEST);
			writeResponse(ctx, req, res);
			return;
		}

		// deny PUT TRACE methods.
		if (req.getMethod() == HttpMethod.PUT || req.getMethod() == HttpMethod.TRACE) {
			DefaultFullHttpResponse res = new DefaultFullHttpResponse(req.getProtocolVersion(), FORBIDDEN);
			writeResponse(ctx, req, res);
			return;
		}
		if ("/favicon.ico".equals(req.getUri())) {
			DefaultFullHttpResponse res = new DefaultFullHttpResponse(req.getProtocolVersion(), HttpResponseStatus.OK);
			IChipInfo cinfo = (IChipInfo) parent.getService("$.chipinfo");
			InputStream gatewayLogo = cinfo.getIconStream();
			byte[] buf = FileHelper.readFully(gatewayLogo);
			res.content().writeBytes(buf);
			writeResponse(ctx, req, res);
			return;
		}
		// Send the demo page and favicon.ico
		if (!isWebSocketReq(req)) {
			flowHttpRequest(ctx, req);
			return;
		}

		// Handshake
		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req),
				null, false, 10485760);
		handshaker = wsFactory.newHandshaker(req);
		if (handshaker == null) {
			WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());
		} else {
			FullHttpRequestImpl freq = new FullHttpRequestImpl(req);
			handshaker.handshake(ctx.channel(), freq);
			websocketActive(ctx, freq);
		}
	}

	protected void flowWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame wsFrame) throws Exception {
		// Check for closing frame
		if (wsFrame instanceof CloseWebSocketFrame) {
			handshaker.close(ctx.channel(), (CloseWebSocketFrame) wsFrame.retain());
			return;
		}
		if (wsFrame instanceof PingWebSocketFrame) {
			ctx.channel().write(new PongWebSocketFrame(wsFrame.content().retain()));
			return;
		}

		ByteBuf bb = null;
		if (wsFrame instanceof TextWebSocketFrame) {
			TextWebSocketFrame f = (TextWebSocketFrame) wsFrame;
			bb = f.content();
		} else if (wsFrame instanceof BinaryWebSocketFrame) {
			BinaryWebSocketFrame f = (BinaryWebSocketFrame) wsFrame;
			bb = f.content();
		} else {
			throw new EcmException("不支持此类消息：" + wsFrame.getClass());
		}

		byte[] b = new byte[bb.readableBytes()];
		bb.readBytes(b);
		Frame frame = new Frame(b);
		String root = frame.rootName();
		if (!currentUsedGatewayDestForHttp.equals(root)) {
			frame.url(String.format("/%s%s", currentUsedGatewayDestForHttp, frame.url()));
		}

		IInputPipeline inputPipeline = pipelines.get(currentUsedGatewayDestForHttp);
		if (inputPipeline != null) {
			Circuit circuit = new Circuit(String.format("%s 200 OK", frame.protocol()));
			inputPipeline.headFlow(frame, circuit);
			return;
		}

		pipelineBuild(currentUsedGatewayDestForHttp, frame, ctx);
	}

	protected void pipelineBuild(String gatewayDest, Frame frame, ChannelHandlerContext ctx) throws Exception {
		WebsocketServerChannelGatewaySocket wsSocket = new WebsocketServerChannelGatewaySocket(parent, ctx.channel());
		sockets.add(wsSocket);// 不放在channelActive方法内的原因是当有构建需要时才添加，是按需索求

		IGatewaySocket socket = this.sockets.getAndCreate(gatewayDest);

		String pipelineName = SocketName.name(ctx.channel().id(), gatewayDest);
		IInputPipelineBuilder builder = (IInputPipelineBuilder) socket.getService("$.pipeline.input.builder");
		IInputPipeline inputPipeline = builder.name(pipelineName).prop(__pipeline_fromProtocol, "ws")
				.prop(__pipeline_fromWho, info.getName()).createPipeline();
		pipelines.add(gatewayDest, inputPipeline);

		ForwardJunction junction = new ForwardJunction(pipelineName);
		junction.parse(inputPipeline, ctx.channel(), socket);
		this.junctions.add(junction);

		Circuit circuit = new Circuit(String.format("%s 200 OK", frame.protocol()));
		inputPipeline.headOnActive(pipelineName);// 通知管道激活

		inputPipeline.headFlow(frame, circuit);// 再把本次请求发送处理
	}

	protected void websocketActive(ChannelHandlerContext ctx, FullHttpRequestImpl req) throws Exception {
		this.currentUsedGatewayDestForHttp = req.getContentPath();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		String name = SocketName.name(ctx.channel().id(), info.getName());
		if (sockets.contains(name)) {// 移除channelSocket而不是destination的socket
			sockets.remove(name);// 不论是ws还是http增加的，在此安全移除
		}

		Set<String> dests = pipelines.enumDest();
		for (String dest : dests) {
			String pipelineName = SocketName.name(ctx.channel().id(), dest);
			Junction junction = junctions.findInForwards(pipelineName);
			if (junction != null) {
				this.junctions.remove(junction);
			}
			IInputPipeline input = pipelines.get(dest);
			input.headOnInactive(pipelineName);
		}
		pipelines.dispose();

		super.channelInactive(ctx);
	}

	protected void flowHttpRequest(ChannelHandlerContext ctx, HttpRequest req) throws Exception {
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

		HttpRequestArgs args = new HttpRequestArgs(ctx, keepLive);

		IInputPipeline inputPipeline = pipelines.get(gatewayDest);
		// 检查目标管道是否存在
		if (inputPipeline != null) {
			inputPipeline.headFlow(req, args);
			this.decoder = args.getDecoder();
			this.visitor = args.getVisitor();
			return;
		}
		// 目标管道不存在，以下生成目标管道
		// 检查并生成目标管道
		pipelineBuild(gatewayDest, args, req, ctx);

	}

	protected void pipelineBuild(String gatewayDest, HttpRequestArgs args, HttpRequest req, ChannelHandlerContext ctx)
			throws Exception {
		IGatewaySocket socket = this.sockets.getAndCreate(gatewayDest);
		String pipelineName = SocketName.name(ctx.channel().id(), gatewayDest);
		IInputPipelineBuilder builder = (IInputPipelineBuilder) socket.getService("$.pipeline.input.builder");
		IInputPipeline inputPipeline = builder.name(pipelineName).prop(__pipeline_fromProtocol, "http")
				.prop(__pipeline_fromWho, info.getName()).createPipeline();
		pipelines.add(gatewayDest, inputPipeline);
		this.currentUsedGatewayDestForHttp = gatewayDest;

		ForwardJunction junction = new ForwardJunction(pipelineName);
		junction.parse(inputPipeline, ctx.channel(), socket);
		this.junctions.add(junction);

		inputPipeline.headOnActive(pipelineName);// 通知管道激活

		inputPipeline.headFlow(req, args);// 将当前请求发过去

		this.decoder = args.getDecoder();
		this.visitor = args.getVisitor();
	}

	protected void writeResponse(ChannelHandlerContext ctx, HttpRequest req, DefaultFullHttpResponse res) {
		HttpHeaders headers = res.headers();
		setContentLength(res, res.content().readableBytes());
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
					}
				}
			}
		}

		ChannelFuture f = ctx.writeAndFlush(res);
		boolean close = req.headers().contains(CONNECTION, HttpHeaders.Values.CLOSE, true)
				|| req.getProtocolVersion().equals(HttpVersion.HTTP_1_0)
						&& !req.headers().contains(CONNECTION, HttpHeaders.Values.KEEP_ALIVE, true);
		if (close) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}

	protected boolean isWebSocketReq(HttpRequest req) {
		if (req.headers().get("Connection") == null)
			return false;
		return ((req.headers().get("Connection").contains("Upgrade"))
				&& "websocket".equalsIgnoreCase(req.headers().get("Upgrade")));
	}

	protected String getWebSocketLocation(HttpRequest req) {
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
				logger.error(this.getClass(), c.getStatus() + " " + c.getMessage());
			} else {
				logger.error(this.getClass(), c);
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

			DefaultFullHttpResponse res = null;
			CircuitException c = ((CircuitException) e);
			if ("404".equals(c.getStatus())) {
				HttpResponseStatus status = new HttpResponseStatus(Integer.parseInt(c.getStatus()),
						HttpResponseStatus.NOT_FOUND.reasonPhrase());
				res = new DefaultFullHttpResponse(HTTP_1_1, status);
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				cause.printStackTrace(pw);
			} else {
				String msg = e.getMessage().replace("\r", "").replace("\n", "<br/>");// netty的HttpResponseStatus构造对\r\n作了错误异常
				HttpResponseStatus status = new HttpResponseStatus(Integer.parseInt(c.getStatus()), msg);
				res = new DefaultFullHttpResponse(HTTP_1_1, status);
			}
			printError(e, res);
			DefaultHttpRequest req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
			res.headers().add(HttpHeaders.Names.CONTENT_TYPE, DefaultHttpMineTypeFactory.mime("html"));
			writeResponse(ctx, req, res);

		} else {
			String error = e.getMessage();
			if (!StringUtil.isEmpty(error)) {
				error.replace("\r", "").replace("\n", "<br/>");
			} else {
				error = "";
			}
			HttpResponseStatus status = new HttpResponseStatus(503, error);
			DefaultFullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, status);
			printError(e, res);
			DefaultHttpRequest req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
			res.headers().add(HttpHeaders.Names.CONTENT_TYPE, DefaultHttpMineTypeFactory.mime("html"));
			writeResponse(ctx, req, res);
		}
		ctx.close();
	}

	private void printError(Throwable e, DefaultFullHttpResponse res) {
		StringWriter out = new StringWriter();
		PrintWriter writer = new PrintWriter(out);
		if (!(e instanceof CircuitException)) {
			out.append(String.format("%s", res.getStatus().toString()));
		}
		e.printStackTrace(writer);
		StringBuffer sb = out.getBuffer();
		res.content().writeBytes(sb.toString().getBytes());
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

	class FullHttpRequestImpl extends DefaultHttpRequest implements FullHttpRequest {
		ByteBuf content;

		public FullHttpRequestImpl(HttpVersion httpVersion, HttpMethod method, String uri, boolean validateHeaders) {
			super(httpVersion, method, uri, validateHeaders);
			content = Unpooled.buffer();
		}

		public FullHttpRequestImpl(HttpVersion httpVersion, HttpMethod method, String uri) {
			super(httpVersion, method, uri);
			content = Unpooled.buffer();
		}

		public FullHttpRequestImpl(HttpRequest req) {
			super(req.getProtocolVersion(), req.getMethod(), req.getUri());
			HttpHeaders headers = req.headers();
			for (Entry<String, String> en : headers.entries()) {
				this.headers().add(en.getKey(), en.getValue());
			}
		}

		public String getContentPath() {
			String uri = getUri();
			while (uri.startsWith("/")) {
				uri = uri.substring(1, uri.length());
			}
			int pos = uri.indexOf("/");
			if (pos < 0) {
				return "";
			}
			return uri.substring(0, pos);
		}

		@Override
		public HttpHeaders trailingHeaders() {
			return super.headers();
		}

		@Override
		public ByteBuf content() {
			return content;
		}

		@Override
		public int refCnt() {
			return content.refCnt();
		}

		@Override
		public boolean release() {
			return content.release();
		}

		@Override
		public boolean release(int arg0) {
			return content.release(arg0);
		}

		@Override
		public FullHttpRequest copy() {
			return this;
		}

		@Override
		public FullHttpRequest duplicate() {
			return this;
		}

		@Override
		public FullHttpRequest retain() {
			return this;
		}

		@Override
		public FullHttpRequest retain(int arg0) {
			return this;
		}

		@Override
		public FullHttpRequest setMethod(HttpMethod arg0) {
			super.setMethod(arg0);
			return this;
		}

		@Override
		public FullHttpRequest setProtocolVersion(HttpVersion arg0) {
			super.setProtocolVersion(arg0);
			return this;
		}

		@Override
		public FullHttpRequest setUri(String arg0) {
			super.setUri(arg0);
			return this;
		}

	}
}
