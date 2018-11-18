package cj.studio.gateway.socket.visitor;

import cj.studio.gateway.socket.IChunkVisitor;

public abstract class AbstractHttpPostVisitor implements IChunkVisitor {

	public abstract IHttpFormDecoder createFormDataDecoder();
	
}
