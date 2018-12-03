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

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.gateway.ICluster;
import cj.studio.gateway.IGatewaySocketContainer;
import cj.studio.gateway.IJunctionTable;
import cj.studio.gateway.conf.ServerInfo;
import cj.studio.gateway.junction.ForwardJunction;
import cj.studio.gateway.junction.Junction;
import cj.studio.gateway.socket.Destination;
import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import cj.studio.gateway.socket.pipeline.IInputPipelineBuilder;
import cj.studio.gateway.socket.pipeline.InputPipelineCollection;
import cj.studio.gateway.socket.util.SocketContants;
import cj.studio.gateway.socket.util.SocketName;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class UdtFrontendHandler extends ChannelHandlerAdapter implements SocketContants {

	private IServiceProvider parent;
	IGatewaySocketContainer sockets;
	private IJunctionTable junctions;
	InputPipelineCollection pipelines;
	private ServerInfo info;
	private Destination destination;

	public UdtFrontendHandler(IServiceProvider parent) {
		this.parent = parent;
		sockets = (IGatewaySocketContainer) parent.getService("$.container.socket");
		junctions = (IJunctionTable) parent.getService("$.junctions");
		this.pipelines = new InputPipelineCollection();
		info = (ServerInfo) parent.getService("$.server.info");
		ICluster cluster = (ICluster) parent.getService("$.cluster");
		this.destination = (Destination) cluster.getDestination(info.getRoad());
		if (destination == null) {
			throw new EcmException("目标中不存在路：" + info.getRoad());
		}
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		String pipelineName = SocketName.name(ctx.channel().id(), info.getName());
		pipelineBuild(pipelineName, ctx);
	}

	protected void pipelineBuild(String pipelineName, ChannelHandlerContext ctx) throws Exception {
		String gatewayDest = destination.getName();
		IGatewaySocket socket = this.sockets.getAndCreate(gatewayDest);

		IInputPipelineBuilder builder = (IInputPipelineBuilder) socket.getService("$.pipeline.input.builder");
		IInputPipeline inputPipeline = builder.name(pipelineName).prop(__pipeline_builder_frontend_channel,ctx.channel()).prop(__pipeline_fromProtocol, "tcp")
				.prop(__pipeline_fromWho, info.getName()).createPipeline();
		pipelines.add(pipelineName, inputPipeline);

		ForwardJunction junction = new ForwardJunction(pipelineName);
		junction.parse(inputPipeline, ctx.channel(), socket);
		this.junctions.add(junction);

		inputPipeline.headOnActive(pipelineName);// 通知管道激活
	}

	@Override
	public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
		String name = SocketName.name(ctx.channel().id(), info.getName());
		IInputPipeline inputPipeline = pipelines.get(name);
		inputPipeline.headFlow(msg, ctx);
	}

	protected void pipelineRelease(String pipelineName) throws Exception {

		Junction junction = junctions.findInForwards(pipelineName);
		if (junction != null) {
			this.junctions.remove(junction);
		}

		IInputPipeline input = pipelines.get(pipelineName);
		if (input != null) {
			input.headOnInactive(pipelineName);
			pipelines.remove(pipelineName);
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		String name = SocketName.name(ctx.channel().id(), info.getName());

		if (sockets.contains(name)) {
			sockets.remove(name);// 在此安全移除
		}

		pipelineRelease(name);
		super.channelInactive(ctx);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
	}
}
