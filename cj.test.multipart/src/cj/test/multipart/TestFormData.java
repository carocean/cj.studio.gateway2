package cj.test.multipart;

import java.io.IOException;
import java.io.RandomAccessFile;

public class TestFormData {

	public static void main(String[] args) throws IOException {
		RandomAccessFile file = new RandomAccessFile("/Users/caroceanjofers/studio/lns.github.com/cj.studio.gateway2/cj.test.multipart/data/formdata.txt", "r");
		int b=0;
		String boundary="---------------------------66463493214568540771885412692";
		IHttpFormDecoder decoder = new HttpFormDecoder(boundary);
		while((b=file.read())>-1) {
			decoder.write((byte)b);
		}
		file.close();
	}

}
