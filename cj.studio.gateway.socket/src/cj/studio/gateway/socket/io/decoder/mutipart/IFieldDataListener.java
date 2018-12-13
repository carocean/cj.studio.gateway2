package cj.studio.gateway.socket.io.decoder.mutipart;
/**
 * 侦听一个域的打开、完成、写入过程
 * @author caroceanjofers
 *
 */
//因为表单是流式解析，所以在整个侦听期间也是流式的，故而可以在open时得到域，在done、wirte时默认也是针对此域
public interface IFieldDataListener {
	
	void openFD(IFieldInfo field);
	
	void doneFD();

	void writeFD(byte b);

}
