package cj.studio.ecm.net.io;

import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IContentReciever;
import cj.studio.ecm.net.IInputChannel;

public class SimpleInputChannel implements IInputChannel {

	private IContentReciever reciever;
	private long writedBytes;
	private Frame frame;

	@Override
	public void flush() throws CircuitException{

	}
	@Override
	public Frame frame() {
		return frame;
	}
	@Override
	public void writeBytes(byte[] b) throws CircuitException{
		writeBytes(b, 0, b.length);
	}

	@Override
	public void writeBytes(byte[] b, int pos, int length) throws CircuitException{
		reciever.recieve(b, pos, length);
		writedBytes += length - pos;
	}

	@Override
	public Frame begin(Object request) {
		this.frame = (Frame) request;
		return frame;
	}

	@Override
	public void done(byte[] b, int pos, int length)throws CircuitException {
		reciever.done(b, pos, length);
		reciever = null;
		frame = null;
	}

	@Override
	public long writedBytes() {
		return writedBytes;
	}

	@Override
	public void accept(IContentReciever reciever) {
		if(reciever==this.reciever)return;
		reciever.begin(frame);
		this.reciever = reciever;
	}

}
