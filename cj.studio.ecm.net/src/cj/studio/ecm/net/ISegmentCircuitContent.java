package cj.studio.ecm.net;

public interface ISegmentCircuitContent extends ICircuitContent {
	Frame createFirst(String frame_line) throws CircuitException;
	void done(byte[] b,int pos,int length)throws CircuitException;
}
