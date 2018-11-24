package cj.studio.gateway.socket.app.valve;

import static io.netty.handler.codec.http.HttpHeaders.setContentLength;

import java.util.Set;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.net.web.HttpCircuit;
import cj.studio.ecm.net.web.HttpFrame;
import cj.studio.gateway.http.args.HttpContentArgs;
import cj.studio.gateway.http.args.HttpRequestArgs;
import cj.studio.gateway.http.args.WebsocketFrameArgs;
import cj.studio.gateway.server.util.DefaultHttpMineTypeFactory;
import cj.studio.gateway.socket.IChunkVisitor;
import cj.studio.gateway.socket.pipeline.IIPipeline;
import cj.studio.gateway.socket.pipeline.IInputValve;
import cj.studio.gateway.socket.util.SocketContants;
import cj.studio.gateway.socket.visitor.AbstractHttpPostVisitor;
import cj.studio.gateway.socket.visitor.AbstractHttpGetVisitor;
import cj.studio.gateway.socket.visitor.HttpWriter;
import cj.studio.gateway.socket.visitor.IHttpFormChunkDecoder;
import cj.studio.gateway.socket.visitor.IHttpWriter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

public class FirstWayInputValve implements IInputValve, SocketContants {
	public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
	public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
	HttpRequest request;

	public FirstWayInputValve() {
	}

	@Override
	public void onActive(String inputName, IIPipeline pipeline) throws CircuitException {
		pipeline.nextOnActive(inputName, this);
	}

	@Override
	public void onInactive(String inputName, IIPipeline pipeline) throws CircuitException {
		pipeline.nextOnInactive(inputName, this);
	}

	@Override
	public void flow(Object request, Object args, IIPipeline pipeline) throws CircuitException {
		if (request instanceof HttpRequest) {
			request = (HttpRequest) request;
			flowHttpRequest(request, (HttpRequestArgs) args/* http 过来的是ctx */, pipeline);
			return;
		}
		if (request instanceof HttpContent) {// 处理http数据块，last块
			flowHttpContent(request, (HttpContentArgs) args/* http 过来的是ctx */, pipeline);
			return;
		}
		if (request instanceof WebSocketFrame) {
			flowWs(request, (WebsocketFrameArgs) args, pipeline);
			return;
		}
		if (request instanceof Frame) {
			Frame f = (Frame) request;
			f.head(__frame_fromProtocol, pipeline.prop(__pipeline_fromProtocol));
			f.head(__frame_fromWho, pipeline.prop(__pipeline_fromWho));
			f.head(__frame_fromPipelineName, pipeline.prop(__pipeline_name));
			pipeline.nextFlow(request, args, this);
			return;
		}
	}

	private void flowHttpContent(Object request, HttpContentArgs args, IIPipeline pipeline) {
		ChannelHandlerContext ctx = args.getContext();
		if (request instanceof LastHttpContent) {
			IHttpFormChunkDecoder decoder = args.getDecoder();
			if (decoder != null) {
				if (request instanceof DefaultLastHttpContent) {
					DefaultLastHttpContent cnt = (DefaultLastHttpContent) request;
					decoder.writeChunk(cnt.content());
				}
				IHttpWriter writer = new HttpWriter(ctx.channel());
				try {
					decoder.done(writer);
				} finally {
					writer.close();
				}
			}
			IChunkVisitor visitor = args.getVisitor();
			if (visitor != null) {
				visitor.endVisit(new HttpWriter(ctx.channel()));
			}
			ChannelFuture f = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
			if (args.isKeepLive()) {
				f.addListener(ChannelFutureListener.CLOSE);
			}
			args.dispose();
			return;
		}
		// 接收内容
		IHttpFormChunkDecoder decoder = args.getDecoder();
		if (decoder != null) {
			HttpContent cnt = (HttpContent) request;
			decoder.writeChunk(cnt.content());
		}
		
	}

	private void flowHttpRequest(Object request, HttpRequestArgs args, IIPipeline pipeline) throws CircuitException {
		HttpRequest req = (HttpRequest) request;
		String uri = req.getUri();
		Frame frame = convertToFrame(uri, req);
		frame.head(__frame_fromProtocol, pipeline.prop(__pipeline_fromProtocol));
		frame.head(__frame_fromWho, pipeline.prop(__pipeline_fromWho));
		frame.head(__frame_fromPipelineName, pipeline.prop(__pipeline_name));
		Circuit circuit = new HttpCircuit(String.format("%s 200 OK", req.getProtocolVersion().text()));
		pipeline.nextFlow(frame, circuit, this);

		ChannelHandlerContext ctx = args.getContext();

		if (circuit.content().readableBytes() > 2 * 1024 * 1024) {
			throw new CircuitException("503", "回路内容超过上限2M，请使用IChunkVisitor机制");
		}
		HttpResponseStatus st = new HttpResponseStatus(Integer.valueOf(circuit.status()), circuit.message());
		DefaultHttpResponse res = new DefaultHttpResponse(HttpVersion.valueOf(frame.protocol()), st);

		IChunkVisitor visitor = (IChunkVisitor) circuit.attribute(__circuit_chunk_visitor);
		if (visitor == null) {
			doWithoutVisitor(ctx, req, res, circuit);
			return;
		}
		// 以下是处理块
		if (visitor instanceof AbstractHttpGetVisitor) {
			AbstractHttpGetVisitor http = (AbstractHttpGetVisitor) visitor;
			args.setVisitor(http);
			try {
				http.beginVisit(frame, circuit);
				doHttpPullChunkVisitor(http, ctx, req, res, circuit);
			} catch (Exception e) {
				throw e;
			} finally {
				http.close();
			}
			return;
		}
		if (visitor instanceof AbstractHttpPostVisitor) {
			if(!HttpMethod.POST.equals(req.getMethod())) {
				throw new CircuitException("505", "非POST请求使用了HttpFormDataVisitor。请求："+frame);
			}
			AbstractHttpPostVisitor formdata = (AbstractHttpPostVisitor) visitor;
			args.setVisitor(formdata);
			try {
				formdata.beginVisit(frame, circuit);
				doHttpFormDataVisitor(formdata, args, req, res, circuit);
			} catch (Exception e) {
				throw e;
			}
			return;
		}
		if(HttpMethod.POST.equals(req.getMethod())) {
			throw new CircuitException("505", "POST请求未使用HttpFormDataVisitor。请求："+frame);
		}
	}

	private void doWithoutVisitor(ChannelHandlerContext ctx, HttpRequest req, DefaultHttpResponse res,
			Circuit circuit) {
		if (HttpMethod.POST.equals(req.getMethod())) {
			CJSystem.logging().warn(getClass(), "POST请求未使用HttpFormDataVisitor：" + req.getUri());
		}
		HttpHeaders headers = res.headers();
		String names[] = circuit.enumHeadName();
		for (String name : names) {
			String v = circuit.head(name);
			headers.add(name, v);
		}
		setContentLength(res, circuit.content().readableBytes());
		String ctypeKey = HttpHeaders.Names.CONTENT_TYPE.toString();
		if (circuit.containsContentType()) {
			res.headers().set(HttpHeaders.Names.CONTENT_TYPE, circuit.contentType());
		} else {
			String extName = req.getUri();
			int pos = extName.lastIndexOf(".");
			extName = extName.substring(pos + 1, extName.length());
			if (DefaultHttpMineTypeFactory.containsMime(extName)) {
				headers.add(ctypeKey, DefaultHttpMineTypeFactory.mime(extName));
			}else{
				String mime = "text/html; charset=utf-8";
				res.headers().set(HttpHeaders.Names.CONTENT_TYPE, mime);
			}
		}
		ctx.writeAndFlush(res);
		if (circuit.content().readableBytes() > 0) {
			DefaultHttpContent data = new DefaultHttpContent(circuit.content().raw());
			ctx.writeAndFlush(data);
		}
	}

	private void doHttpFormDataVisitor(AbstractHttpPostVisitor visitor, HttpRequestArgs args, HttpRequest req,
			DefaultHttpResponse res, Circuit circuit) {
		ChannelHandlerContext ctx = args.getContext();
		long len = circuit.content().readableBytes();
		HttpHeaders.setContentLength(res, len);

		if (circuit.containsContentType()) {
			res.headers().set(HttpHeaders.Names.CONTENT_TYPE, circuit.contentType());
		} else {
			String extName = req.getUri();
			int pos = extName.lastIndexOf(".");
			extName = extName.substring(pos + 1, extName.length());
			if (DefaultHttpMineTypeFactory.containsMime(extName)) {
				String mime = DefaultHttpMineTypeFactory.mime(extName);
				res.headers().set(HttpHeaders.Names.CONTENT_TYPE, mime);
			}else{
				String mime = "text/html; charset=utf-8";
				res.headers().set(HttpHeaders.Names.CONTENT_TYPE, mime);
			}
		}

		ctx.writeAndFlush(res);
		if (circuit.content().readableBytes() > 0) {
			HttpContent chunk = new DefaultHttpContent(circuit.content().raw());
			ctx.writeAndFlush(chunk);
		}

		IHttpFormChunkDecoder decoder = visitor.createFormDataDecoder();
		args.setDecoder(decoder);
	}

	private void doHttpPullChunkVisitor(AbstractHttpGetVisitor visitor, ChannelHandlerContext ctx, HttpRequest req,
			HttpResponse res, Circuit circuit) {
		long len = visitor.getContentLength() + circuit.content().readableBytes();
		HttpHeaders.setContentLength(res, len);

		if (circuit.containsContentType()) {
			res.headers().set(HttpHeaders.Names.CONTENT_TYPE, circuit.contentType());
		} else {
			String extName = req.getUri();
			int pos = extName.lastIndexOf(".");
			extName = extName.substring(pos + 1, extName.length());
			if (DefaultHttpMineTypeFactory.containsMime(extName)) {
				String mime = DefaultHttpMineTypeFactory.mime(extName);
				res.headers().set(HttpHeaders.Names.CONTENT_TYPE, mime);
			}else{
				String mime = "text/html; charset=utf-8";
				res.headers().set(HttpHeaders.Names.CONTENT_TYPE, mime);
			}
		}

		ctx.writeAndFlush(res);
		if (circuit.content().readableBytes() > 0) {
			HttpContent chunk = new DefaultHttpContent(circuit.content().raw());
			ctx.writeAndFlush(chunk);
		}
		int read = 0;
		byte[] b = new byte[SocketContants.__pull_chunk_size];
		while ((read = visitor.readChunk(b, 0, b.length)) > -1) {
			ByteBuf buf = Unpooled.buffer(read);
			buf.writeBytes(b, 0, read);
			HttpContent chunk = new DefaultHttpContent(buf);
			ctx.writeAndFlush(chunk);
		}
	}

	private Frame convertToFrame(String uri, HttpRequest req) throws CircuitException {
		String line = String.format("%s %s %s", req.getMethod(), uri, req.getProtocolVersion().text());
		Frame f = new HttpFrame(line);
		HttpHeaders headers = req.headers();
		Set<String> set = headers.names();
		for (String key : set) {
			if ("url".equals(key)) {
				continue;
			}
			String v = headers.get(key);
			f.head(key, v);
		}
		return f;
	}

	private void flowWs(Object req, WebsocketFrameArgs args, IIPipeline pipeline) throws CircuitException {
		WebSocketFrame request = (WebSocketFrame) req;
		ByteBuf bb = request.content();
		byte[] b = new byte[bb.readableBytes()];
		bb.readBytes(b);
		Frame f = new Frame(b);
		String sname = pipeline.prop(__pipeline_toWho);
		String uri = "";
		if (f.containsQueryString()) {
			uri = String.format("/%s%s?%s", sname, f.path(), f.queryString());
		} else {
			uri = String.format("/%s%s", sname, f.path());
		}
		f.url(uri);
		f.head(__frame_fromProtocol, pipeline.prop(__pipeline_fromProtocol));
		f.head(__frame_fromWho, pipeline.prop(__pipeline_fromWho));
		f.head(__frame_fromPipelineName, pipeline.prop(__pipeline_name));
		Circuit c = new Circuit(String.format("%s 200 OK", f.protocol()));
		pipeline.nextFlow(f, c, this);
		c.dispose();
	}
}
