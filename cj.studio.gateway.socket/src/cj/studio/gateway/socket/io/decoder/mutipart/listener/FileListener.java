package cj.studio.gateway.socket.io.decoder.mutipart.listener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import cj.studio.gateway.socket.io.decoder.mutipart.IFieldDataListener;
import cj.studio.gateway.socket.io.decoder.mutipart.IFieldInfo;

public class FileListener implements IFieldDataListener {
	RandomAccessFile currentFile;
	byte[] cache;
	int index;
	private String storePath;
	private int cacheSize;
	public FileListener(String storePath) {
		this(storePath,0);
	}
	/**
	 * 
	 * @param storePath 接收的文件的存储路径
	 * @param cacheSize 缓冲大小，如果小于等于零将默认65536字节
	 */
	public FileListener(String storePath,int cacheSize) {
		if(!storePath.endsWith(File.separator)) {
			storePath=storePath+File.separator;
		}
		this.storePath=storePath;
		if(cacheSize<=0) {
			cacheSize=65536;
		}
		this.cacheSize=cacheSize;
	}
	@Override
	public void writeFD(byte b) {
		if (currentFile == null) {
			return;
		}
		cache[index] = b;
		index++;
		if (index >= cacheSize) {
			flush();
		}
	}

	private void flush() {
		try {
			currentFile.write(cache, 0, index);
		} catch (IOException e) {
			e.printStackTrace();
		}
		index = 0;
	}

	@Override
	public void openFD(IFieldInfo field) {
		if (field.isFile()) {
			cache = new byte[cacheSize];
			index = 0;
			try {
				currentFile = new RandomAccessFile(storePath + field.filename(), "rw");
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}

	}

	@Override
	public void doneFD() {
		if (currentFile == null) {
			return;
		}
		if (index > 0) {
			flush();
		}
		index = 0;
		try {
			currentFile.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			currentFile = null;
			
		}

	}

}
