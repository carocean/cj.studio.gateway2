package cj.studio.gateway.socket.visitor.decoder.mutipart;

import java.io.UnsupportedEncodingException;

import cj.ultimate.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class AttributeInfo implements IAttributeInfo {
	ByteBuf data;
	private String info;

	public AttributeInfo() {
		data = Unpooled.buffer();
	}

	@Override
	public void write(byte b) {
		data.writeByte(b);
	}

	@Override
	public void end() {
	}

	@Override
	public String getInfo() {
		if (StringUtil.isEmpty(info)) {
			byte[] b = new byte[data.readableBytes()];
			data.getBytes(0, b);
			try {
				info = new String(b, "utf-8");
			} catch (UnsupportedEncodingException e) {
			}
		}
		return info;
	}
	
	@Override
	public AttributeType getType() {
		String str = getInfo();
		if (str.startsWith("Content-Disposition")) {
			return AttributeType.ContentDisposition;
		}
		if (str.startsWith("Content-Type")) {
			return AttributeType.ContentType;
		}
		if (str.startsWith("Content-Transfer-Encoding")) {
			return AttributeType.ContentTransferEncoding;
		}
		return null;
	}
	@Override
	public String toString() {
		return getInfo();
	}
}
