package cj.studio.gateway.socket.visitor.decoder.mutipart;

public interface ISiever {
	int end();
	void write(byte b, IBucket bucket);
	byte[] cache();
	boolean hasCache();
}
