/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package cj.studio.gateway.road.http;

import cj.studio.ecm.IServiceProvider;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;

public class HttpServerInitializer extends ChannelInitializer<SocketChannel> {

	private IServiceProvider parent;


	public HttpServerInitializer(IServiceProvider parent) {
		this.parent= parent;
	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		ch.pipeline().addLast( new HttpRequestDecoder(), new HttpResponseEncoder(),
				new HttpContentCompressor(), new ChunkedWriteHandler(),
				new HttpFrontendHandler(parent));
	}
}
