package cj.studio.gateway.socket.app.valve;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.frame.IFlowContent;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.ecm.net.web.HttpCircuit;
import cj.studio.ecm.net.web.HttpFrame;
import cj.studio.gateway.socket.pipeline.IIPipeline;
import cj.studio.gateway.socket.pipeline.IInputValve;
import cj.studio.gateway.socket.util.SocketContants;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

public class FirstWayInputValve implements IInputValve,SocketContants {
	private long uploadFileLimitLength;

	public FirstWayInputValve(long uploadFileLimitLength) {
		this.uploadFileLimitLength = uploadFileLimitLength;
		if (uploadFileLimitLength <= 0) {
			this.uploadFileLimitLength = 4194304L;// 4M;
		}
	}

	@Override
	public void onActive(String inputName, Object request, Object response, IIPipeline pipeline)
			throws CircuitException {
		if (request instanceof FullHttpRequest) {
			flowHttpOnActive(inputName,request, response, pipeline);
			return;
		}
		if (request instanceof WebSocketFrame) {
			flowWsOnActive(inputName, (WebSocketFrame)request, pipeline);
			return;
		}
		if (request instanceof Frame) {
			Frame f=(Frame)request;
			f.head(__frame_fromProtocol, pipeline.prop(__pipeline_fromProtocol));
			f.head(__frame_fromWho, pipeline.prop(__pipeline_fromWho));
			f.head(__frame_fromPipelineName, pipeline.prop(__pipeline_name));
			pipeline.nextOnActive(inputName,request, response, this);
			return;
		}
	}
	private void flowWsOnActive(String inputName,WebSocketFrame request, IIPipeline pipeline) throws CircuitException {
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
		pipeline.nextOnActive(inputName,f, c, this);
		c.dispose();
	}
	private void flowHttpOnActive(String inputName,Object request, Object response, IIPipeline pipeline) throws CircuitException {
		FullHttpRequest req = (FullHttpRequest) request;
		String uri = req.getUri();
		Frame frame = convertToFrame(uri, req);
		frame.head(__frame_fromProtocol, pipeline.prop(__pipeline_fromProtocol));
		frame.head(__frame_fromWho, pipeline.prop(__pipeline_fromWho));
		frame.head(__frame_fromPipelineName, pipeline.prop(__pipeline_name));
		Circuit circuit = new HttpCircuit(String.format("%s 200 OK", req.getProtocolVersion().text()));
		pipeline.nextOnActive(inputName,frame, circuit, this);
		FullHttpResponse res = (FullHttpResponse) response;
		fillToResponse(circuit, res);
	}
	@Override
	public void onInactive(String inputName, IIPipeline pipeline) throws CircuitException {
		pipeline.nextOnInactive(inputName, this);
	}

	@Override
	public void flow(Object request, Object response, IIPipeline pipeline) throws CircuitException {
		if (request instanceof FullHttpRequest) {
			flowHttp(request, response, pipeline);
			return;
		}
		if (request instanceof WebSocketFrame) {
			flowWs((WebSocketFrame) request, pipeline);
			return;
		}
		if (request instanceof Frame) {
			Frame f=(Frame)request;
			f.head(__frame_fromProtocol, pipeline.prop(__pipeline_fromProtocol));
			f.head(__frame_fromWho, pipeline.prop(__pipeline_fromWho));
			f.head(__frame_fromPipelineName, pipeline.prop(__pipeline_name));
			pipeline.nextFlow(request, response, this);
			return;
		}
	}

	private void flowWs(WebSocketFrame request, IIPipeline pipeline) throws CircuitException {
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

	private void flowHttp(Object request, Object response, IIPipeline pipeline) throws CircuitException {
		FullHttpRequest req = (FullHttpRequest) request;
		String uri = req.getUri();
		Frame frame = convertToFrame(uri, req);
		frame.head(__frame_fromProtocol, pipeline.prop(__pipeline_fromProtocol));
		frame.head(__frame_fromWho, pipeline.prop(__pipeline_fromWho));
		frame.head(__frame_fromPipelineName, pipeline.prop(__pipeline_name));
		Circuit circuit = new HttpCircuit(String.format("%s 200 OK", req.getProtocolVersion().text()));
		pipeline.nextFlow(frame, circuit, this);
		FullHttpResponse res = (FullHttpResponse) response;
		fillToResponse(circuit, res);
	}

	private void fillToResponse(Circuit circuit, FullHttpResponse res) {
		HttpResponseStatus st = new HttpResponseStatus(Integer.valueOf(circuit.status()), circuit.message());
		res.setStatus(st);
		IFlowContent cnt = circuit.content();
		if (cnt.readableBytes() > 0) {
			res.content().writeBytes(cnt.readFully());
		}
		HttpHeaders resheaders = res.headers();
		String names[] = circuit.enumHeadName();
		for (String name : names) {
			String v = circuit.head(name);
			resheaders.add(name, v);
		}
	}

	private Frame convertToFrame(String uri, FullHttpRequest req) throws CircuitException {
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
		HttpPostRequestDecoder decoder = null;
		if (!req.getMethod().equals(HttpMethod.POST)) {
			ByteBuf bbuf = req.content();
			if (bbuf.readableBytes() > 0) {
				f.content().writeBytes(bbuf);
			}
			return f;
		}
		decoder = new HttpPostRequestDecoder(req);
		boolean isMultipart = decoder.isMultipart();
		if (!isMultipart) {
			List<InterfaceHttpData> parmList = decoder.getBodyHttpDatas();
			for (InterfaceHttpData parm : parmList) {
				if (parm.getHttpDataType() == HttpDataType.Attribute) {
					Attribute data = (Attribute) parm;
					try {
						f.parameter(data.getName(), data.getValue());
					} catch (IOException e) {
						throw new CircuitException("505", e);
					}
				}
			}
			// 如果表解码后为空（即附合post协议规范的无）而请求仍然有内容，则说明是不附合post协议规范的请求（那么就是属gateway特有的功能），应将数据写到侦内容
			if (parmList.isEmpty() && req.content().readableBytes() > 0) {
				ByteBuf bbuf = req.content();
				f.content().writeBytes(bbuf);
			}
			return f;
		}

		decoder.offer(req);
		f.head("Is-Multipart", String.valueOf(isMultipart));
		List<InterfaceHttpData> parmList = decoder.getBodyHttpDatas();
		for (InterfaceHttpData parm : parmList) {
			if (parm.getHttpDataType() == HttpDataType.Attribute) {
				Attribute data = (Attribute) parm;
				try {
					f.parameter(data.getName(), data.getValue());
				} catch (IOException e) {
					throw new CircuitException("505", e);
				}
			} else {
				if (parm.getHttpDataType() == HttpDataType.FileUpload) {
					FileUpload fileUpload = (FileUpload) parm;// httpserver使用了聚合器Aggregator，因此此处的类型必为：DiskFileUpload或混合模式
					f.head("Is-Completed", String.valueOf(fileUpload.isCompleted()));
					f.head("is-InMemory", String.valueOf(fileUpload.isInMemory()));
					if (fileUpload.isCompleted()) {

						f.head("File-Length", String.valueOf(fileUpload.length()));
						/*
						 * 上传文件最大大小， 这是gatewaysocket上的限制 ， httpserver也有限制为 ：Aggregator- Limit
						 */
						if (fileUpload.length() < uploadFileLimitLength) {
							f.head("File-Charset", fileUpload.getCharset().toString());
							f.parameter(fileUpload.getName(), fileUpload.getFilename());
							try {

								if (!fileUpload.isInMemory()) {
									f.head("Disk-File", fileUpload.getFile().getAbsolutePath());
								}
								ByteBuf bbuf = fileUpload.getByteBuf();
								if (bbuf.readableBytes() > 0) {
									f.content().writeBytes(bbuf);
								}
							} catch (IOException e) {
								throw new CircuitException("505", e);
							}

						} else {
							throw new CircuitException("505",
									"\tFile too long to be printed out:" + fileUpload.length() + "\r\n");
						}
					}
				}
			}
		}
		req.content().resetReaderIndex();
		if (parmList.isEmpty() && req.content().readableBytes() > 0) {// 为了支持gateway在非http post协议规范的情况下的提交
			ByteBuf bbuf = req.content();
			f.content().writeBytes(bbuf);
		}

		return f;
	}
}
