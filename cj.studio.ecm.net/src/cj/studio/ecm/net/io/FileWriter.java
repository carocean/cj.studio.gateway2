package cj.studio.ecm.net.io;

import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.IContentWriter;
import cj.studio.ecm.net.IOutputChannel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FileWriter implements IContentWriter {
    boolean finished;
	RandomAccessFile raf;
    public FileWriter(File file) throws FileNotFoundException {
		raf = new RandomAccessFile(file, "r");
        this.finished=false;
    }

	@Override
	public boolean isFinished() {
		return finished;
	}

	@Override
    public void write(IOutputChannel output) throws CircuitException {
		try {
			int read = 0;
			byte[] b = new byte[8192];
			while ((read = raf.read(b)) != -1) {
				output.write(b, 0, read);
			}
		} catch (IOException e) {
			throw new CircuitException("503", e);
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException e) {
				}
			}
			finished=true;
		}
    }

	@Override
	public long length() {
		try {
			return raf.length();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
