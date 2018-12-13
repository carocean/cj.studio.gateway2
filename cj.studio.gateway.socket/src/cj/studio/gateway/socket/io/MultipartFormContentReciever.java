package cj.studio.gateway.socket.io;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IContentReciever;
import cj.studio.gateway.socket.io.decoder.mutipart.Bucket;
import cj.studio.gateway.socket.io.decoder.mutipart.IFieldDataListener;
import cj.studio.gateway.socket.io.decoder.mutipart.IFormData;
import cj.studio.gateway.socket.io.decoder.mutipart.IMultipartFormDecoder;
import cj.studio.gateway.socket.io.decoder.mutipart.MultipartFormDecoder;
import cj.ultimate.util.StringUtil;

public abstract class MultipartFormContentReciever implements IContentReciever{
	private IMultipartFormDecoder decoder;
	private Frame frame;
	private Bucket bucket;
	private boolean isMultipart;
	@Override
	public void begin(Frame frame) {
		String ctype = frame.contentType();// application/x-www-form-urlencoded
		if (ctype.contains("multipart/")) {
			isMultipart = true;
		} else {
			return;
		}
		this.frame = frame;
		String[] keypaires = ctype.split(";");
		String bdstr = "";
		for (String keypair : keypaires) {
			String item[] = keypair.split("=");
			if (item.length < 2) {
				continue;
			}
			if ("boundary".equals(item[0].trim())) {
				bdstr = item[1];
				break;
			}
		}
		if (StringUtil.isEmpty(bdstr)) {
			throw new EcmException("侦头Content-Type中缺少boundary");
		}
		IFieldDataListener listener=createFieldDataListener();
		this.bucket=new Bucket(listener);
		decoder=new MultipartFormDecoder(bdstr,bucket);
	}
	@Override
	public void recieve(byte[] b, int pos, int length) {
		if(!isMultipart)return;
		writeBytes(b,pos,length);
	}
	
	protected  void writeBytes(byte[] b, int pos, int length) {
		for(int i=pos;i<length;i++) {
			decoder.write(b[i]);
		}
	}
	@Override
	public void done(byte[] b, int pos, int length) {
		if(!isMultipart)return;
		writeBytes(b, pos, length);
		
		IFormData form=bucket.getForm();
		done(frame,form);
		frame = null;
	}
	protected abstract void done(Frame frame, IFormData form);
	protected abstract IFieldDataListener createFieldDataListener();
	
}
