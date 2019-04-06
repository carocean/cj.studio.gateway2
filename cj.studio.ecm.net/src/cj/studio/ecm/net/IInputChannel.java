package cj.studio.ecm.net;

public interface IInputChannel {
	Frame frame();
	void writeBytes(byte[] b,int pos,int length) throws CircuitException;
	void writeBytes(byte[] b)throws CircuitException;
	Frame begin(Object request)throws CircuitException;
	void done(byte[] b,int pos,int length)throws CircuitException;
	long writedBytes()throws CircuitException;
	void flush() throws CircuitException;
	void accept(IContentReciever reciever)throws CircuitException;
	boolean isDone();
}
