package cj.studio.gateway.socket.visitor;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.gateway.socket.visitor.decoder.MultipartFormChunkDecoder;
import cj.studio.gateway.socket.visitor.decoder.UrlEncodedFormChunkDecoder;

public abstract class HttpPostVisitor extends AbstractHttpPostVisitor {
	boolean isMultipart;
	private Frame frame;
	private Circuit circuit;

	@Override
	public void beginVisit(Frame frame, Circuit circuit) {
		String ctype = frame.contentType();// application/x-www-form-urlencoded
		if (ctype.contains("multipart/")) {
			isMultipart = true;
		} else {
			isMultipart = false;
		}
		this.frame = frame;
		this.circuit = circuit;
	}

	@Override
	public final void endVisit(IHttpVisitorWriter writer) {
		endvisit(frame, circuit, writer);
		frame = null;
		circuit = null;
	}

	protected abstract void endvisit(Frame frame, Circuit circuit, IHttpVisitorWriter writer);

	@Override
	public final IHttpFormChunkDecoder createFormDataDecoder() {
		IHttpFormChunkDecoder decoder = null;
		if (isMultipart) {
			decoder = createMultipartFormDecoder(frame, circuit);
			if (decoder == null) {
				decoder = new MultipartFormChunkDecoder(frame, circuit);
			}
			return decoder;
		}
		decoder = createUrlencodedFormDecoder(frame, circuit);
		if (decoder == null) {
			decoder = new UrlEncodedFormChunkDecoder(frame, circuit);
		}
		return decoder;
	}

	protected IHttpFormChunkDecoder createUrlencodedFormDecoder(Frame frame, Circuit circuit) {
		return null;
	}

	protected abstract IHttpFormChunkDecoder createMultipartFormDecoder(Frame frame, Circuit circuit);

}
