package cj.studio.gateway.http.args;

import cj.studio.gateway.socket.IChunkVisitor;
import cj.studio.gateway.socket.visitor.IHttpFormDecoder;
import cj.ultimate.IDisposable;
import io.netty.channel.ChannelHandlerContext;

public class HttpContentArgs implements IDisposable{
	boolean keepLive;
	ChannelHandlerContext context;
	IHttpFormDecoder decoder;
	IChunkVisitor visitor;
	boolean isDisposed;
	public HttpContentArgs(ChannelHandlerContext ctx,IHttpFormDecoder decoder,IChunkVisitor visitor, boolean keepLive) {
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
	public IHttpFormDecoder getDecoder() {
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
