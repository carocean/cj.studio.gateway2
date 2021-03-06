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
package cj.studio.gateway.road.udt;

import cj.studio.ecm.IServiceProvider;
import cj.studio.gateway.road.tcp.TcpFrontendHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public class UdtServerInitializer extends ChannelInitializer<SocketChannel> {

	private IServiceProvider parent;


	public UdtServerInitializer(IServiceProvider parent) {
		this.parent= parent;
	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		ch.pipeline().addLast(
				new TcpFrontendHandler(parent));
	}
}
