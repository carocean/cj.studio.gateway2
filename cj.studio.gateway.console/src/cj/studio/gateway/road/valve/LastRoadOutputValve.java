package cj.studio.gateway.road.valve;

import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.socket.pipeline.IOPipeline;
import cj.studio.gateway.socket.pipeline.IOutputValve;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

public class LastRoadOutputValve implements IOutputValve {
	IServiceProvider parent;
	Channel frontend;
	Channel backend;
	public LastRoadOutputValve(IServiceProvider parent, Channel frontend,Channel backend) {
		this.parent = parent;
		this.frontend = frontend;
		this.backend=backend;
	}

	@Override
	public void flow(Object request, Object response, IOPipeline pipeline) throws CircuitException {
		frontend.writeAndFlush(request).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                	backend.read();
                } else {
                    future.channel().close();
                }
            }
        });
	}

	@Override
	public void onActive(IOPipeline pipeline) throws CircuitException {
		frontend.read();
		frontend.write(Unpooled.EMPTY_BUFFER);
	}

	@Override
	public void onInactive(IOPipeline pipeline) throws CircuitException {
//		inputPipelines.remove(name);
	}

}
