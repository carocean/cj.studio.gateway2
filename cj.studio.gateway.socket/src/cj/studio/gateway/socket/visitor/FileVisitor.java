package cj.studio.gateway.socket.visitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.gateway.socket.IChunkVisitor;

public class FileVisitor extends AbstractHttpGetVisitor implements IChunkVisitor {
	private RandomAccessFile file;

	public FileVisitor(File f) {
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
	@Override
	public void beginVisit(Frame frame, Circuit circuit) {
		
		
	}
	@Override
	public void endVisit(IHttpWriter writer) {
		
		
	}
}
