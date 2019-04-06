package cj.studio.gateway.socket.io;

import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IContentReciever;
import cj.studio.ecm.net.IInputChannel;

public class UdtInputChannel implements IInputChannel {

	private Frame frame;
	private IContentReciever reciever;
	private long writedBytes;
	boolean isDone;
	@Override
	public Frame frame() {
		return frame;
	}
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
	public Frame begin(Object request) throws CircuitException {
		Frame pack=(Frame)request;
		byte[] b=pack.content().readFully();
		Frame frame=new Frame(this,b);
		this.frame=frame;
		isDone=false;
		return frame;
	}

	@Override
	public void done(byte[] b, int pos, int length)throws CircuitException {
		if (reciever != null) {
			reciever.done(b, pos, length);
		}
		writedBytes += length - pos;
		isDone=true;
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
	@Override
	public boolean isDone() {
		return isDone;
	}
}
