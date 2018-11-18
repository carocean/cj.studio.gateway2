package cj.test.website.file;

import java.nio.charset.Charset;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;

public class MyHttpPostRequestDecoder extends HttpPostRequestDecoder{

	public MyHttpPostRequestDecoder(HttpDataFactory factory, HttpRequest request, Charset charset) {
		super(factory, request, charset);
		// TODO Auto-generated constructor stub
	}

	public MyHttpPostRequestDecoder(HttpDataFactory factory, HttpRequest request) {
		super(factory, request);
		// TODO Auto-generated constructor stub
	}

	public MyHttpPostRequestDecoder(HttpRequest request) {
		super(request);
		// TODO Auto-generated constructor stub
	}
	
}
