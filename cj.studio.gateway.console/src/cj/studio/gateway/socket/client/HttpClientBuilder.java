package cj.studio.gateway.socket.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;

class HttpClientBuilder implements IHttpClientBuilder {

	private int maxIdleConnections;
	private boolean retryOnConnectionFailure;
	private long writeTimeout;
	private long readTimeout;
	private boolean followRedirects;
	private long connectTimeout;
	private long keepAliveDuration;
	private OkHttpClient client;

	@Override
	public IHttpClientBuilder maxIdleConnections(int maxIdleConnections) {
		this.maxIdleConnections=maxIdleConnections;
		return this;
	}

	@Override
	public IHttpClientBuilder keepAliveDuration(long keepAliveDuration) {
		this.keepAliveDuration=keepAliveDuration;
		return this;
	}

	@Override
	public IHttpClientBuilder connectTimeout(long connectTimeout) {
		this.connectTimeout=connectTimeout;
		return this;
	}

	@Override
	public IHttpClientBuilder followRedirects(boolean followRedirects) {
		this.followRedirects=followRedirects;
		return this;
	}

	@Override
	public IHttpClientBuilder readTimeout(long readTimeout) {
		this.readTimeout=readTimeout;
		return this;
	}

	@Override
	public IHttpClientBuilder writeTimeout(long writeTimeout) {
		this.writeTimeout=writeTimeout;
		return this;
	}

	@Override
	public IHttpClientBuilder retryOnConnectionFailure(boolean retryOnConnectionFailure) {
		this.retryOnConnectionFailure=retryOnConnectionFailure;
		return this;
	}

	@Override
	public OkHttpClient build(ExecutorService exe) {
		if(client!=null) {
			return client;
		}
		ConnectionPool pool = new ConnectionPool(maxIdleConnections, keepAliveDuration, TimeUnit.MILLISECONDS);
		Dispatcher dispatcher = new Dispatcher(exe);
		this.client = new OkHttpClient().newBuilder()//
				.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS) //
				.followRedirects(followRedirects) //
				.readTimeout(readTimeout, TimeUnit.MILLISECONDS) //
				.retryOnConnectionFailure(retryOnConnectionFailure) //
				.writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)//
				.dispatcher(dispatcher).connectionPool(pool).build();

		return client;
	}

}
