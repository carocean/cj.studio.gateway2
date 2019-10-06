package cj.studio.gateway.socket.io;

import java.util.Set;

import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IContentReciever;
import cj.studio.ecm.net.IInputChannel;
import cj.studio.ecm.net.http.HttpFrame;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;

public class HttpInputChannel implements IInputChannel {

	private Frame frame;
	private IContentReciever reciever;
	private long writedBytes;
	boolean isDone;
	@Override
	public void writeBytes(byte[] b, int pos, int length) throws CircuitException {
		if (reciever != null) {
			reciever.recieve(b, pos, length);
		}
		writedBytes += length - pos;
	}

	@Override
	public void writeBytes(byte[] b)throws CircuitException {
		writeBytes(b, 0, b.length);

	}
	@Override
	public Frame frame() {
		return frame;
	}
	@Override
	public Frame begin(Object request) {
		HttpRequest req = (HttpRequest) request;
		String uri = req.uri();
		String line = String.format("%s %s %s", req.method(), uri, req.protocolVersion().text());
		Frame f = new HttpFrame(this, line);
		HttpHeaders headers = req.headers();
		Set<CharSequence> set = headers.names();
		for (CharSequence key : set) {
			if ("url".equals(key)) {
				continue;
			}
			String v = headers.get(key).toString();
			f.head(key.toString(), v);
		}
		this.frame=f;
		isDone=false;
		return f;
	}

	@Override
	public void done(byte[] b, int pos, int length)throws CircuitException {
		writedBytes += length - pos;
		if (reciever != null) {
			reciever.done(b, pos, length);
		}
		isDone=true;
	}
	@Override
	public boolean isDone() {
		return isDone;
	}
	@Override
	public long writedBytes() {
		return writedBytes;
	}

	@Override
	public void accept(IContentReciever reciever) {
		reciever.begin(frame);
		this.reciever = reciever;
	}

	@Override
	public void flush() {

	}

	public Frame getFrame() {
		return frame;
	}

}
