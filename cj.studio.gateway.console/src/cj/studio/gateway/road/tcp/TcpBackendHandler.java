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
package cj.studio.gateway.road.tcp;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.socket.pipeline.IOutputPipeline;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class TcpBackendHandler extends ChannelHandlerAdapter {
	private final IOutputPipeline output;

	public TcpBackendHandler(IOutputPipeline output) {
		this.output = output;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ctx.read();
		ctx.write(Unpooled.EMPTY_BUFFER);
		output.headOnActive();
	}

	@Override
	public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
		output.headFlow(msg, ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		try {
			output.headOnInactive();
		} catch (CircuitException e) {
			throw new EcmException(e);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
	}
}
