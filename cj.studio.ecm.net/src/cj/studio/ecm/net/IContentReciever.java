package cj.studio.ecm.net;

public interface IContentReciever {

	void recieve(byte[] b, int pos, int length) throws CircuitException;

	void done(byte[] b, int pos, int length) throws CircuitException;
	void begin(Frame frame);
}
