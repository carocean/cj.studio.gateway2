package cj.studio.ecm.net;

public interface IOutputChannel {
	void write(byte[] b,int pos,int length);
	void begin(Circuit circuit);//circuit or frame
	void done(byte[] b,int pos,int length);
	long writedBytes();
}
