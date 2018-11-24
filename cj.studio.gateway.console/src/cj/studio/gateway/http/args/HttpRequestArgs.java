package cj.studio.gateway.http.args;

import cj.studio.gateway.socket.IChunkVisitor;
import cj.studio.gateway.socket.visitor.IHttpFormChunkDecoder;
import cj.ultimate.IDisposable;
import io.netty.channel.ChannelHandlerContext;

public class HttpRequestArgs implements IDisposable{
	boolean keepLive;
	ChannelHandlerContext context;
	IHttpFormChunkDecoder decoder;
	IChunkVisitor visitor;
	public HttpRequestArgs(ChannelHandlerContext ctx,boolean keepLive) {
		super();
		this.keepLive = keepLive;
		this.context=ctx;
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
	public void setDecoder(IHttpFormChunkDecoder decoder) {
		this.decoder = decoder;
	}
	public void setVisitor(IChunkVisitor visitor) {
		this.visitor = visitor;
	}
	public IChunkVisitor getVisitor() {
		return visitor;
	}
	@Override
	public void dispose() {
		this.decoder=null;
		this.context=null;
		this.visitor=null;
	}
}
