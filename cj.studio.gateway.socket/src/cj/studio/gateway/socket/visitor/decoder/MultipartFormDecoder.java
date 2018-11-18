package cj.studio.gateway.socket.visitor.decoder;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.gateway.socket.visitor.IHttpFormDecoder;
import cj.studio.gateway.socket.visitor.IHttpWriter;
import cj.ultimate.util.StringUtil;
import io.netty.buffer.ByteBuf;

public class MultipartFormDecoder implements IHttpFormDecoder {

	private Frame frame;
	private Circuit circuit;
	HttpFormData formData;
	HttpFormBoundary bd;

	public MultipartFormDecoder(Frame frame, Circuit circuit) {
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
		bd = new HttpFormBoundary(String.format("--%s",bdstr));
		formData = new HttpFormData();
	}

	@Override
	public void done(IHttpWriter writer) {
		System.out.println(formData);
		HttpFieldData[] arr=formData.fields.toArray(new HttpFieldData[0]);
		for(HttpFieldData f:arr) {
			if(f.isFile()) {
				frame.parameter(f.getName(),new String(f.filename()));
				continue;
			}
			if(f.isMixed()) {
				HttpFormData fd=f.childForm();
				frame.parameter(f.getName(),new String(f.getName()));
				continue;
			}
			frame.parameter(f.getName(),new String(f.data()));
		}
		frame = null;
		circuit = null;
	}
	
	@Override
	public void writeChunk(ByteBuf chunk) {
		byte[] data = new byte[chunk.readableBytes()];
		chunk.readBytes(data);
		for (byte b : data) {
			formData.write(b, bd);
		}
	}

}
