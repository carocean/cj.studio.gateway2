package cj.studio.gateway.socket.client;

import java.util.concurrent.ExecutorService;

import okhttp3.OkHttpClient;

public interface IHttpClientBuilder {

	IHttpClientBuilder maxIdleConnections(int maxIdleConnections);

	IHttpClientBuilder keepAliveDuration(long keepAliveDuration);

	IHttpClientBuilder connectTimeout(long connectTimeout);

	IHttpClientBuilder followRedirects(boolean followRedirects);

	IHttpClientBuilder readTimeout(long readTimeout);

	IHttpClientBuilder writeTimeout(long writeTimeout);

	IHttpClientBuilder retryOnConnectionFailure(boolean retryOnConnectionFailure);

	OkHttpClient build(ExecutorService exe);

}
