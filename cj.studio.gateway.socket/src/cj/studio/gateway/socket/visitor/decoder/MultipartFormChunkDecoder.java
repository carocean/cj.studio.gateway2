package cj.studio.gateway.socket.visitor.decoder;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.gateway.socket.util.SocketContants;
import cj.studio.gateway.socket.visitor.IHttpFormChunkDecoder;
import cj.studio.gateway.socket.visitor.IHttpWriter;
import cj.studio.gateway.socket.visitor.decoder.mutipart.Bucket;
import cj.studio.gateway.socket.visitor.decoder.mutipart.IFieldDataListener;
import cj.studio.gateway.socket.visitor.decoder.mutipart.IFieldInfo;
import cj.studio.gateway.socket.visitor.decoder.mutipart.IFormData;
import cj.studio.gateway.socket.visitor.decoder.mutipart.IMultipartFormDecoder;
import cj.studio.gateway.socket.visitor.decoder.mutipart.MultipartFormDecoder;
import cj.ultimate.util.StringUtil;
import io.netty.buffer.ByteBuf;

public class MultipartFormChunkDecoder implements IHttpFormChunkDecoder,SocketContants {
	private IMultipartFormDecoder decoder;
	private Frame frame;
	private Circuit circuit;
	private Bucket bucket;
	public MultipartFormChunkDecoder(Frame frame, Circuit circuit) {
		this.frame = frame;
		this.circuit = circuit;
		String ctype = frame.contentType();
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

	protected IFieldDataListener createFieldDataListener() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public final void done(IHttpWriter writer) {
		IFormData form=bucket.getForm();
		String keys[]=form.enumFieldName();
		for(String name:keys) {
			IFieldInfo field=form.getFieldInfo(name);
			field.name();
		}
		done(frame, circuit, writer);
		frame = null;
		circuit = null;
	}
	
	protected  void done(Frame frame, Circuit circuit, IHttpWriter writer) {
	}
	@Override
	public void writeChunk(ByteBuf chunk) {
		while(chunk.isReadable()) {
			byte b=chunk.readByte();
			decoder.write(b);
		}
	}

}
