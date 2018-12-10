package cj.studio.gateway.socket.cable.wire;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.graph.CircuitException;
import cj.studio.gateway.socket.cable.IGatewaySocketWire;
import cj.studio.gateway.socket.client.IExecutorPool;
import cj.ultimate.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

//okhttp: https://www.jianshu.com/p/da4a806e599b
public class HttpGatewaySocketWire implements IGatewaySocketWire {
	IServiceProvider parent;
	volatile boolean isIdle;
	private long idleBeginTime;
	private OkHttpClient client;
	private String domain;

	public HttpGatewaySocketWire(IServiceProvider parent) {
		this.parent = parent;
	}

	@Override
	public void close() {
		@SuppressWarnings("unchecked")
		List<IGatewaySocketWire> wires = (List<IGatewaySocketWire>) parent.getService("$.wires");
		wires.remove(this);
	}

	@Override
	public void used(boolean b) {
		isIdle = !b;
		idleBeginTime = System.currentTimeMillis();
	}

	@Override
	public void dispose() {
		close();
	}

	@Override
	public long idleBeginTime() {
		return idleBeginTime;
	}

	@Override
	public boolean isIdle() {
		return isIdle;
	}

	@Override
	public Object send(Object request, Object response) throws CircuitException {
		Frame frame = (Frame) request;
		String fullUri = String.format("%s%s", domain, frame.retrieveUrl());
		Request.Builder rbuilder = new Request.Builder();

		String[] names = frame.enumHeadName();
		for (String name : names) {
			if ("command".equals(name) || "url".equals(name) || "protocol".equals(name)) {
				continue;
			}
			rbuilder.addHeader(name, frame.head(name));
		}
		if (!frame.containsHead("Connection")) {
			rbuilder.addHeader("Connection", "Keep-Alive");
		}

		String host = frame.head("Host");
		if (StringUtil.isEmpty(host)) {
			int pos = domain.indexOf("://");
			host = domain.substring(pos + 3, domain.length());
			rbuilder.addHeader("Host", host);
		}
		String contentType = frame.contentType();
		if (StringUtil.isEmpty(frame.contentType())) {
			contentType = "text/html; charset=utf-8";
		}
		MediaType mt = MediaType.parse(contentType);

		if ("POST".equals(frame.command().toUpperCase())) {
			if (frame.content().readableBytes() > 0) {
				byte[] data = frame.content().readFully();
				RequestBody body = RequestBody.create(mt, data);
				rbuilder.post(body);
			}
		} else if ("GET".equals(frame.command().toUpperCase())) {
			rbuilder.get();
		} else {
			throw new CircuitException("503", "不支持的http方法:" + frame.command());
		}
		Request req = rbuilder.url(fullUri)// 默认就是GET请求，可以不写
				.build();
		Circuit circuit = (Circuit) response;
		if (circuit.hasFeedback()) {// 异步
			circuit.beginFeeds();
			Call call = client.newCall(req);
			call.enqueue(new Callback() {
				ByteBuf content = Unpooled.buffer();

				@Override
				public void onFailure(Call call, IOException e) {
					content.clear();
					CircuitException exc = new CircuitException("505", e);
					content.writeBytes(exc.messageCause().getBytes());
					circuit.doneFeeds(content);
				}

				@Override
				public void onResponse(Call call, Response response) throws IOException {
					content.clear();
					ResponseBody body = response.body();
					InputStream in = body.byteStream();
					int len = 0;
					byte[] b = new byte[content.writableBytes()];
					while ((len = in.read(b, 0, b.length)) != -1) {
						content.writeBytes(b, 0, len);
					}
					try {
						circuit.writeFeeds(content);
					} catch (CircuitException e) {
						throw new IOException(e.getMessage());
					}
					if (response.isSuccessful()) {
						circuit.doneFeeds(content);
						content.release();
					}
				}
			});
			return circuit;
		}
		// 以下是同步
		Call call = client.newCall(req);
		try {
			Response res = call.execute();
			convertToCircuit(circuit, res);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return circuit;
	}

	private void convertToCircuit(Circuit circuit, Response res) throws CircuitException {
		Headers headers = res.headers();
		Set<String> set = headers.names();
		for (String key : set) {
			String v = headers.get(key);
			circuit.head(key, v);
		}
		circuit.message(res.message());
		circuit.status(res.code() + "");
		ResponseBody body = res.body();
		if (body != null) {
			circuit.contentType(body.contentType().toString());
			circuit.head("Content-Length", body.contentLength() + "");
			try {
				circuit.content().writeBytes(body.bytes());
			} catch (IOException e) {
				throw new CircuitException("503", e);
			}
		}
	}

	@Override
	public void connect(String ip, int port) throws CircuitException {
		int maxIdleConnections = (int) parent.getService("$.prop.maxIdleConnections");
		long keepAliveDuration = (long) parent.getService("$.prop.keepAliveDuration");
		long connectTimeout = (long) parent.getService("$.prop.connectTimeout");
		boolean followRedirects = (boolean) parent.getService("$.prop.followRedirects");
		long readTimeout = (long) parent.getService("$.prop.readTimeout");
		long writeTimeout = (long) parent.getService("$.prop.writeTimeout");
		boolean retryOnConnectionFailure = (boolean) parent.getService("$.prop.retryOnConnectionFailure");

		this.domain = String.format("%s://%s:%s", parent.getService("$.prop.protocol"), ip, port);

		IExecutorPool exepool = (IExecutorPool) parent.getService("$.executor.pool");
		ExecutorService exe = exepool.getExecutor();
		ConnectionPool pool = new ConnectionPool(maxIdleConnections, keepAliveDuration, TimeUnit.MILLISECONDS);
		Dispatcher dispatcher = new Dispatcher(exe);
		this.client = new OkHttpClient().newBuilder()//
				.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS) //
				.followRedirects(followRedirects) //
				.readTimeout(readTimeout, TimeUnit.MILLISECONDS) //
				.retryOnConnectionFailure(retryOnConnectionFailure) //
				.writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)//
				.dispatcher(dispatcher).connectionPool(pool).build();

		used(false);
	}

	@Override
	public boolean isWritable() {
		return isOpened();
	}

	@Override
	public boolean isOpened() {
		ExecutorService exe = client.dispatcher().executorService();
		return !exe.isTerminated() && !exe.isShutdown();
	}

}
