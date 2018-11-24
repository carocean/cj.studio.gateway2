package cj.studio.gateway.http.args;

import cj.studio.gateway.socket.IChunkVisitor;
import cj.studio.gateway.socket.visitor.IHttpFormChunkDecoder;
import cj.ultimate.IDisposable;
import io.netty.channel.ChannelHandlerContext;

public class HttpContentArgs implements IDisposable{
	boolean keepLive;
	ChannelHandlerContext context;
	IHttpFormChunkDecoder decoder;
	IChunkVisitor visitor;
	boolean isDisposed;
	public HttpContentArgs(ChannelHandlerContext ctx,IHttpFormChunkDecoder decoder,IChunkVisitor visitor, boolean keepLive) {
		super();
		this.keepLive = keepLive;
		this.context=ctx;
		this.decoder=decoder;
		this.visitor=visitor;
	}
	
	public boolean isKeepLive() {
		return keepLive;
	}
	public void setKeepLive(boolean keepLive) {
		this.keepLive = keepLive;
	}
	public ChannelHandlerContext getContext() {
		return context;
	}
	public void setContext(ChannelHandlerContext context) {
		this.context = context;
	}
	public IHttpFormChunkDecoder getDecoder() {
		return decoder;
	}
	public boolean isDisposed() {
		return isDisposed;
	}
	public IChunkVisitor getVisitor() {
		return visitor;
	}
	@Override
	public void dispose() {
		this.decoder=null;
		this.context=null;
		this.visitor=null;
		isDisposed=true;
	}
}
