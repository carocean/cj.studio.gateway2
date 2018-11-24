package cj.test.multipart;

import java.io.IOException;
import java.io.RandomAccessFile;

public class TestFormData {

	public static void main(String[] args) throws IOException {
		RandomAccessFile file = new RandomAccessFile(
				"/Users/caroceanjofers/studio/lns.github.com/cj.studio.gateway2/cj.test.multipart/data/mixed.txt", "r");
		int b = 0;
		String boundary = "---------------------------66463493214568540771885412692";
//		String boundary="AaB03x";
		IBucket bucket = new Bucket(new IFieldDataListener() {

			@Override
			public void writeFD(byte b) {
			}

			@Override
			public void openFD(IFieldInfo field) {
				if (field.isFile()) {
					System.out.println("--" + field.filename());
				}

			}

			@Override
			public void doneFD() {

			}
		});
		for (int i = 0; i < 2000; i++) {
			IMultipartFormDecoder decoder = new MultipartFormDecoder(boundary, bucket);
			file.seek(0);
			while ((b = file.read()) > -1) {
				decoder.write((byte) b);
			}
		}
//		IFormData form = bucket.getForm();
//		String[] names = form.enumFieldName();
//		for (String name : names) {
//			IFieldInfo f = form.getFieldInfo(name);
//			if (f.getChildForm() != null) {
//				String[] keys = f.getChildForm().enumFieldName();
//				for (String key : keys) {
//					IFieldInfo f2 = f.getChildForm().getFieldInfo(key);
//					System.out.println("这是子域" + f2.value());
//				}
//			} else {
//				System.out.println(f.value());
//			}
//		}
		file.close();
	}

}
