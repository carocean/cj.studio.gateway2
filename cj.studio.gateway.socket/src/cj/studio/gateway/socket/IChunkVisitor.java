package cj.studio.gateway.socket;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.gateway.socket.visitor.IHttpWriter;

public interface IChunkVisitor {
	void beginVisit(Frame frame,Circuit circuit);
	void endVisit(IHttpWriter writer);
}
