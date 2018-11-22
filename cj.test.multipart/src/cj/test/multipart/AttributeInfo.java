package cj.test.multipart;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class AttributeInfo implements IAttributeInfo {
	ByteBuf data;
	public AttributeInfo() {
		data=Unpooled.buffer();
	}
	@Override
	public void write(byte b) {
		data.writeByte(b);
	}
	
	@Override
	public void end() {
		byte[] bb=new byte[data.readableBytes()];
		data.getBytes(0,bb);
		System.out.println(new String(bb));
		System.out.println("<属性完>");
	}

}
