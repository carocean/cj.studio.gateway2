package cj.studio.gateway.server.handler;

import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;

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
import cj.studio.ecm.logging.ILogging;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IInputChannel;
import cj.studio.ecm.net.IOutputChannel;
import cj.studio.ecm.net.http.HttpCircuit;
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.ecm.net.io.MemoryInputChannel;
import cj.studio.gateway.IGatewaySocketContainer;
import cj.studio.gateway.IJunctionTable;
import cj.studio.gateway.conf.ServerInfo;
import cj.studio.gateway.junction.ForwardJunction;
import cj.studio.gateway.junction.Junction;
import cj.studio.gateway.server.util.DefaultHttpMineTypeFactory;
import cj.studio.gateway.server.util.GetwayDestHelper;
import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.io.HttpInputChannel;
import cj.studio.gateway.socket.io.HttpOutputChannel;
import cj.studio.gateway.socket.io.InnerWSOutputChannel;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import cj.studio.gateway.socket.pipeline.IInputPipelineBuilder;
import cj.studio.gateway.socket.pipeline.InputPipelineCollection;
import cj.studio.gateway.socket.serverchannel.ws.WebsocketServerChannelGatewaySocket;
import cj.studio.gateway.socket.util.SocketContants;
import cj.studio.gateway.socket.util.SocketName;
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
import io.netty.handler.codec.http.LastHttpContent;
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
	private String currentUsedGatewayDestForHttp;
	private HttpInputChannel inputChannel;
	private Circuit circuit;

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
			handleHttpContent(ctx, msg);
		} else if (msg instanceof WebSocketFrame) {
			flowWebSocketFrame(ctx, (WebSocketFrame) msg);
		} else {
			throw new CircuitException("801", "不支持的消息类型：" + msg.getClass());
		}
	}

	private void handleHttpContent(ChannelHandlerContext ctx, Object msg) throws CircuitException {
		if (inputChannel == null) {
			return;
		}
		if (msg instanceof LastHttpContent) {
			LastHttpContent cnt = (LastHttpContent) msg;
			byte[] b = new byte[cnt.content().readableBytes()];
			cnt.content().readBytes(b, 0, b.length);
			try {
				inputChannel.done(b, 0, b.length);
				circuit.content().flush();// 到此刷新
			} catch (Exception e) {
				if (!circuit.content().isCommited()) {
					CircuitException ce = CircuitException.search(e);
					if (ce != null) {
						circuit.status(ce.getStatus());
					} else {
						circuit.status("503");
					}
					circuit.message(e.toString().replace("\r", "").replace("\n", ""));
					circuit.content().clearbuf();
					circuit.content().flush();
				}
				StringWriter out = new StringWriter();
				e.printStackTrace(new PrintWriter(out));
				circuit.content().writeBytes(out.toString().getBytes());
				circuit.content().flush();
				throw e;
			} finally {
				if (!circuit.content().isClosed()) {
					circuit.content().close();
				}
			}
			return;
		}
		// 以下接收数据
		HttpContent cnt = (HttpContent) msg;
		byte[] b = new byte[cnt.content().readableBytes()];
		cnt.content().readBytes(b, 0, b.length);
		try {
			inputChannel.writeBytes(b, 0, b.length);
		} catch (Exception e) {
			if (!circuit.content().isCommited()) {
				CircuitException ce = CircuitException.search(e);
				if (ce != null) {
					circuit.status(ce.getStatus());
				} else {
					circuit.status("503");
				}
				circuit.message(e.toString().replace("\r", "").replace("\n", ""));
				circuit.content().clearbuf();
				circuit.content().flush();
			}
			StringWriter out = new StringWriter();
			e.printStackTrace(new PrintWriter(out));
			circuit.content().writeBytes(out.toString().getBytes());
			throw e;
		}
	}

	private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest req) throws Exception {
		if (!req.getDecoderResult().isSuccess()) {
			DefaultFullHttpResponse res = new DefaultFullHttpResponse(req.getProtocolVersion(), BAD_REQUEST);
			writeResponse(ctx, req, res);
			reset();
			return;
		}

		// deny PUT TRACE methods.
		if (req.getMethod() == HttpMethod.PUT || req.getMethod() == HttpMethod.TRACE) {
			DefaultFullHttpResponse res = new DefaultFullHttpResponse(req.getProtocolVersion(), FORBIDDEN);
			writeResponse(ctx, req, res);
			reset();
			return;
		}
		if ("/favicon.ico".equals(req.getUri())) {
			DefaultFullHttpResponse res = new DefaultFullHttpResponse(req.getProtocolVersion(), HttpResponseStatus.OK);
			IChipInfo cinfo = (IChipInfo) parent.getService("$.chipinfo");
			InputStream gatewayLogo = cinfo.getIconStream();
			byte[] buf = FileHelper.readFully(gatewayLogo);
			res.content().writeBytes(buf);
			writeResponse(ctx, req, res);
			reset();
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
		reset();
	}

	private void reset() {
		this.inputChannel = null;
		this.circuit = null;
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
		IInputChannel input = new MemoryInputChannel(8192);
		MemoryContentReciever rec = new MemoryContentReciever();
		input.accept(rec);
		Frame frame = new Frame(input, b);
		frame.content().accept(rec);
		input.begin(null);
		input.done(b, 0, 0);

		String root = frame.rootName();
		if (!currentUsedGatewayDestForHttp.equals(root)) {
			frame.url(String.format("/%s%s", currentUsedGatewayDestForHttp, frame.url()));
		}

		IOutputChannel output = new InnerWSOutputChannel(ctx.channel(), frame);
		Circuit circuit = new Circuit(output, String.format("%s 200 OK", frame.protocol()));

		IInputPipeline inputPipeline = pipelines.get(currentUsedGatewayDestForHttp);
		if (inputPipeline != null) {
			flowWSPipeline(ctx, inputPipeline, frame, circuit);
			return;
		}

		pipelineWSBuild(currentUsedGatewayDestForHttp, frame, circuit, ctx);
	}

	protected void flowWSPipeline(ChannelHandlerContext ctx, IInputPipeline pipeline, Frame frame, Circuit circuit)
			throws Exception {
		frame.head(__frame_fromProtocol, pipeline.prop(__pipeline_fromProtocol));
		frame.head(__frame_fromWho, pipeline.prop(__pipeline_fromWho));
		frame.head(__frame_fromPipelineName, pipeline.prop(__pipeline_name));

		try {
			pipeline.headFlow(frame, circuit);
		} catch (Throwable e) {
			if (!circuit.content().isCommited()) {
				circuit.content().clearbuf();
				circuit.content().flush();
			}
			StringWriter out = new StringWriter();
			e.printStackTrace(new PrintWriter(out));
			circuit.content().writeBytes(out.toString().getBytes());
			circuit.content().flush();
			throw e;
		} finally {
			if (!circuit.content().isClosed()) {
				circuit.content().close();
			}
		}

	}

	protected void pipelineWSBuild(String gatewayDest, Frame frame, Circuit circuit, ChannelHandlerContext ctx)
			throws Exception {
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

		try {
			inputPipeline.headOnActive(pipelineName);// 通知管道激活
		} catch (Exception e) {
			if (!circuit.content().isCommited()) {
				circuit.content().clearbuf();
				circuit.content().flush();
			}
			StringWriter out = new StringWriter();
			e.printStackTrace(new PrintWriter(out));
			circuit.content().writeBytes(out.toString().getBytes());
			circuit.content().close();
			ctx.close();
			throw e;
		}

		flowWSPipeline(ctx, inputPipeline, frame, circuit);// 再把本次请求发送处理
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

		HttpInputChannel input = new HttpInputChannel();
		Frame frame = input.begin(req);
		IOutputChannel output = new HttpOutputChannel(ctx.channel(), frame);
		Circuit circuit = new HttpCircuit(output, String.format("%s 200 OK", req.getProtocolVersion().text()));
		this.circuit = circuit;
		this.inputChannel = input;

		IInputPipeline inputPipeline = pipelines.get(gatewayDest);
		// 检查目标管道是否存在
		if (inputPipeline != null) {
			flowHttpPipeline(inputPipeline, ctx, frame, circuit);
			return;
		}
		// 目标管道不存在，以下生成目标管道
		// 检查并生成目标管道

		pipelineHttpBuild(gatewayDest, frame, circuit, ctx);
	}

	protected void flowHttpPipeline(IInputPipeline pipeline, ChannelHandlerContext ctx, Frame frame, Circuit circuit)
			throws CircuitException {

		frame.head(__frame_fromProtocol, pipeline.prop(__pipeline_fromProtocol));
		frame.head(__frame_fromWho, pipeline.prop(__pipeline_fromWho));
		frame.head(__frame_fromPipelineName, pipeline.prop(__pipeline_name));

		try {
			pipeline.headFlow(frame, circuit);
		} catch (Throwable e) {
			if (!circuit.content().isCommited()) {
				circuit.content().clearbuf();
				circuit.content().flush();
			}
			StringWriter out = new StringWriter();
			e.printStackTrace(new PrintWriter(out));
			circuit.content().writeBytes(out.toString().getBytes());
			circuit.content().flush();
			throw e;
		}

	}

	protected void pipelineHttpBuild(String gatewayDest, Frame frame, Circuit circuit, ChannelHandlerContext ctx)
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
		try {
			inputPipeline.headOnActive(pipelineName);// 通知管道激活
		} catch (Exception e) {
			if (!circuit.content().isCommited()) {
				circuit.content().clearbuf();
				circuit.content().flush();
			}
			StringWriter out = new StringWriter();
			e.printStackTrace(new PrintWriter(out));
			circuit.content().writeBytes(out.toString().getBytes());
			circuit.content().close();
			ctx.close();
			throw e;
		}
		flowHttpPipeline(inputPipeline, ctx, frame, circuit);// 将当前请求发过去

	}

	protected void writeResponse(ChannelHandlerContext ctx, HttpRequest req, DefaultFullHttpResponse res) {
		HttpHeaders headers = res.headers();
		boolean close = headers.contains(CONNECTION, HttpHeaders.Values.CLOSE, true)
				|| req.getProtocolVersion().equals(HttpVersion.HTTP_1_0)
						&& !headers.contains(CONNECTION, HttpHeaders.Values.KEEP_ALIVE, true);
		if (!close) {
			res.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		}
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
		ChannelFuture f = ctx.channel().writeAndFlush(res);
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

	private void errorHttpCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
		if (e instanceof CircuitException) {
			CircuitException ce = (CircuitException) e;
			if ("404".equals(ce.getStatus())) {
				CJSystem.logging().error(getClass(), e.getMessage());
				return;
			}
		}
		if (e instanceof IOException) {
			if (e.getMessage().indexOf("Connection reset by peer") > -1) {
				ctx.close();
				return;
			}
		}
		CJSystem.logging().error(getClass(), e);
	}

	private void errorWebsocketCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
		if (e instanceof CircuitException) {
			CircuitException ce = (CircuitException) e;
			if ("404".equals(ce.getStatus())) {
				CJSystem.logging().error(getClass(), e.getMessage());
				return;
			}
		}
		if (e instanceof IOException) {
			if (e.getMessage().indexOf("Connection reset by peer") > -1) {
				ctx.close();
				return;
			}
		}
		CJSystem.logging().error(getClass(), e);
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
