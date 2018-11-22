package cj.test.multipart;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Bucket implements IBucket {
	public IAttributeInfo current;
	RandomAccessFile currentFile;

	@Override
	public void doneAttributeInfo() {
		if (current != null) {
			current.end();
		}
	}

	@Override
	public void writeAttributeInfo(byte b) {
		current.write(b);
	}

	@Override
	public void writeFieldData(byte b) {
		System.out.print((char) b);
		try {
			currentFile.writeByte(b);
		} catch (IOException e) {
		}
//		if(this.isMixed()) {//如果data流包含有边界，则说明是mixed模式，含有子表单，则新建解码器解码
//			IHttpFormDecoder decoder=new HttpFormDecoder(this.boundary());
//			decoder.write(b);
//		}
	}

	int index;

	@Override
	public void beginAttributeInfo() {
		System.out.println("<属性创建>");
		IAttributeInfo info = new AttributeInfo();
		this.current = info;
		
	}

	@Override
	public void beginField() {
		try {
			currentFile = new RandomAccessFile("/Users/caroceanjofers/studio/lns.github.com/cj.studio.gateway2/cj.test.multipart/test/" + index, "rw");
			index++;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println("<域开始>");
	}

	@Override
	public void doneField() {
		try {
			if (currentFile.length() < 2) {
				System.out.println("...在此竟然创建了个空属性...");
			}
			currentFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("<域结束>");
	}

	@Override
	public void beginForm() {
		// TODO Auto-generated method stub
		System.out.println("<表单始>");
	}

	@Override
	public void beginFieldData() {
		System.out.println("<域数据始>");
	}

	@Override
	public void doneFieldData() {
		System.out.println("<域数据完>");

	}

	@Override
	public void doneForm() {
		// TODO Auto-generated method stub
		System.out.println("<表单完>");
	}
}
