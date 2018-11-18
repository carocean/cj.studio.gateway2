package cj.studio.gateway.socket.visitor;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.gateway.socket.visitor.decoder.MultipartFormDecoder;
import cj.studio.gateway.socket.visitor.decoder.UrlencodedFormDecoder;

public abstract class HttpPostFreeVisitor extends AbstractHttpPostVisitor {
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
	public final void endVisit(IHttpWriter writer) {
		endvisit(frame, circuit, writer);
		frame = null;
		circuit = null;
	}

	protected abstract void endvisit(Frame frame, Circuit circuit, IHttpWriter writer);

	@Override
	public final IHttpFormDecoder createFormDataDecoder() {
		IHttpFormDecoder decoder = null;
		if (isMultipart) {
			decoder = createMultipartFormDecoder(frame, circuit);
			if (decoder == null) {
				decoder = new MultipartFormDecoder(frame, circuit);
			}
			return decoder;
		}
		decoder = createUrlencodedFormDecoder(frame, circuit);
		if (decoder == null) {
			decoder = new UrlencodedFormDecoder(frame, circuit);
		}
		return decoder;
	}

	protected IHttpFormDecoder createUrlencodedFormDecoder(Frame frame, Circuit circuit) {
		return null;
	}

	protected abstract IHttpFormDecoder createMultipartFormDecoder(Frame frame, Circuit circuit);

}
