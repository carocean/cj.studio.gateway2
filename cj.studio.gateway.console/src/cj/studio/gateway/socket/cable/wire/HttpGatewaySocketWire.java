package cj.studio.gateway.socket.cable.wire;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.socket.cable.IGatewaySocketWire;
import cj.studio.gateway.socket.client.IExecutorPool;
import cj.ultimate.util.StringUtil;
import okhttp3.Call;
import okhttp3.Callback;
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
	OkHttpClient client;
	String domain;

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
		if (isIdle) {
			idleBeginTime = System.currentTimeMillis();
		}
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
		used(true);
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
			if (frame.content().revcievedBytes() > 0) {
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
		if (!circuit.content().isAllInMemory()) {// 异步
			Call call = client.newCall(req);
			call.enqueue(new Callback() {

				@Override
				public void onFailure(Call call, IOException e) {
					CircuitException exc = new CircuitException("505", e);
					byte[] b = exc.messageCause().getBytes();
					circuit.content().writeBytes(b);
				}

				@Override
				public void onResponse(Call call, Response response) throws IOException {
					fillCircut(response);
					ResponseBody body = response.body();
					InputStream in = body.byteStream();
					int len = 0;
					byte[] b = new byte[8192];
					while ((len = in.read(b, 0, b.length)) != -1) {
						circuit.content().writeBytes(b, 0, len);
					}
					if (response.isSuccessful()) {
						circuit.content().close();
						used(false);
					}
				}

				private void fillCircut(Response res) {
					Headers headers = res.headers();
					Set<String> set = headers.names();
					for (String key : set) {
						String v = headers.get(key);
						circuit.head(key, v);
					}
					circuit.message(res.message());
					circuit.status(res.code() + "");

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
		used(false);
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
				circuit.content().close();
			} catch (IOException e) {
				throw new CircuitException("503", e);
			}
		}
	}

	@Override
	public void connect(String ip, int port) throws CircuitException {
		if(client!=null) {
			return;
		}
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
		this.client = exepool.httpClientBuilder().maxIdleConnections(maxIdleConnections)
				.keepAliveDuration(keepAliveDuration).connectTimeout(connectTimeout).followRedirects(followRedirects)
				.readTimeout(readTimeout).writeTimeout(writeTimeout).retryOnConnectionFailure(retryOnConnectionFailure)
				.build(exe);
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
