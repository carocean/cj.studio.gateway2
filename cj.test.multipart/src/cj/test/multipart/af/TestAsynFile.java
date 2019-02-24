package cj.test.multipart.af;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import cj.studio.ecm.CJSystem;

public class TestAsynFile {

	public static void main(String[] args) throws IOException, InterruptedException {
		AsynchronousFileChannel channel = null;
			channel = AsynchronousFileChannel.open(Paths.get("/Users/caroceanjofers/Downloads/2.mkv"), StandardOpenOption.READ);
			ByteBuffer buffer = ByteBuffer.allocate(8192);
			Object obj=new Object();
			channel.read(buffer, 0, obj, new CompletionHandler<Integer, Object>() {
				@Override
				public void completed(Integer result, Object obj) {
					System.out.println("---completed--");
				}

				@Override
				public void failed(Throwable exc, Object attachment) {
					CJSystem.logging().error(getClass(),exc);
				}
			});
			System.out.println("---");
	}

}
