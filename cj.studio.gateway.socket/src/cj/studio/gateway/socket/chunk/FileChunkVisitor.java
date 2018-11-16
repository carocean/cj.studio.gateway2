package cj.studio.gateway.socket.chunk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import cj.studio.ecm.EcmException;
import cj.studio.gateway.socket.IChunkVisitor;

public class FileChunkVisitor extends HttpChunkVisitor implements IChunkVisitor {
	private RandomAccessFile file;

	public FileChunkVisitor(File f) {
		try {
			this.file = new RandomAccessFile(f, "r");
		} catch (FileNotFoundException e) {
			throw new EcmException(e);
		}
	}

	@Override
	public long getContentLength() {
		try {
			return file.length();
		} catch (IOException e) {
			throw new EcmException(e);
		}
	}

	@Override
	public int readChunk(byte[] b, int i, int length) {
		int read = -1;
		try {
			read = file.read(b, 0, length);
		} catch (IOException e) {
			throw new EcmException(e);
		}
		return read;
	}

	public void close() {
		try {
			file.close();
		} catch (IOException e) {
		}
		file = null;
	}

}
