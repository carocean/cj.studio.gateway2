package cj.studio.ecm.net;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.net.io.MemoryOutputChannel;
import io.netty.buffer.ByteBuf;

class DefaultCircuitContent implements ICircuitContent {
	private ByteBuf buf;
	private int capacity;
	IOutputChannel output;
	protected byte state;
	private Circuit owner;
	private ReentrantLock lock;
	private Condition waitClose;
	IContentWriter writer;
	public DefaultCircuitContent(Circuit owner, IOutputChannel output, ByteBuf buf, int capacity) {
		this.buf = buf;
		this.capacity = capacity;
		this.output = output;
		this.owner = owner;
		this.lock = new ReentrantLock();
	}

	@Override
	public void writer(IContentWriter writer) {
		this.writer=writer;
	}

	public DefaultCircuitContent(Circuit owner, IOutputChannel writer, ByteBuf buf) {
		this(owner, writer, buf, 8192);// 默认8K
	}
	@Override
	public ISegmentCircuitContent segment() {
		if(this instanceof ISegmentCircuitContent) {
			return (ISegmentCircuitContent)this;
		}
		return null;
	}
	@Override
	public boolean isAllInMemory() {
		return output instanceof MemoryOutputChannel;
	}

	protected void checkError() {
		if (state == -1) {
			throw new EcmException("流已关闭");
		}
	}

	protected void checkIsFull() {
		if (output == null)
			return;
		if (buf.readableBytes() >= capacity) {
			if (state == 0) {
				output.begin(owner);
				state = 1;
			}
			byte[] b = readFully(buf);
			output.write(b, 0, b.length);
		}
	}

	private byte[] readFully(ByteBuf buf) {
		byte[] b = new byte[buf.readableBytes()];
		buf.readBytes(b, 0, b.length);
		buf.clear();
		return b;
	}

	@Override
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	@Override
	public void clearbuf() {
		buf.clear();
	}

	@Override
	public void flush() {
		if (output == null)
			return;
		if (state == 0) {
			output.begin(owner);
			state = 1;
		}
		if (buf.readableBytes() > 0) {
			byte[] b = readFully(buf);
			output.write(b, 0, b.length);
			return;
		}
		if (writer != null&&!writer.isFinished()) {
			try {
				writer.write(output);
			} catch (CircuitException e) {
				throw new EcmException(e);
			}
		}
	}

	@Override
	public void close() {
		try {
			checkIsFull();
			flush();
			output.done(new byte[0],0,0);
			state = -1;
		} finally {
			if (waitClose != null) {
				try {
					lock.lock();
					waitClose.signalAll();
				} finally {
					lock.unlock();
					waitClose = null;
				}
			}
		}
	}

	@Override
	public void writeBytes(byte[] b) {
		this.writeBytes(b, 0, b.length);
	}

	@Override
	public void writeBytes(byte[] b, int pos, int len) {
		try {
			lock.lock();//每次请求仅生成一次circuitContent实例，故在该实例内上锁不会堵塞别的请求的写操作。
			checkError();
			buf.writeBytes(b, pos, len);
			checkIsFull();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void writeBytes(byte[] b, int pos) {
		this.writeBytes(b, pos, b.length);
	}

	@Override
	public int capacity() {
		return capacity;
	}

	@Override
	public boolean isCommited() {
		return state == 1 ? true : false;
	}

	@Override
	public boolean isReady() {
		return state == 0 ? true : false;
	}

	@Override
	public boolean isClosed() {
		return state == -1 ? true : this.output.isClosed();
	}

	@Override
	public void waitClose() {
		if (waitClose == null) {
			throw new EcmException("未调用beginWait");
		}
		try {
			lock.lock();
			waitClose.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void beginWait() {
		waitClose = lock.newCondition();
	}

	@Override
	public void waitClose(long micSeconds) {
		if (waitClose == null) {
			throw new EcmException("未调用beginWait");
		}
		try {
			lock.lock();
			if (!waitClose.await(micSeconds, TimeUnit.MILLISECONDS)) {
				state = -1;
				CJSystem.logging().warn(getClass(), "等待关闭超时，系统自动关闭了输出通道。等待时间：" + micSeconds);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public long writedBytes() {
		return this.output.writedBytes()+(writer==null?0:writer.length());
	}

}
