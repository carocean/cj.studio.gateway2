package cj.test.multipart;

public interface ISiever {
	int end();
	void write(byte b, IBucket bucket);
	byte[] cache();
	boolean hasCache();
}
