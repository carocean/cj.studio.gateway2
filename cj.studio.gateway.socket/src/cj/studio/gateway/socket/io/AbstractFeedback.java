package cj.studio.gateway.socket.io;

import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IInputChannel;
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.ecm.net.io.MemoryInputChannel;

public abstract class AbstractFeedback implements IFeedback {
	long state;

	@Override
	public final Frame createFirst(String frame_line) throws CircuitException {
		MemoryInputChannel infirst=new MemoryInputChannel(8192);
		Frame first=new Frame(infirst,frame_line);
		first.content().accept(new MemoryContentReciever());
		infirst.begin(first);
		infirst.done(new byte[0], 0, 0);
		state++;
		return first;
	}

	@Override
	public final void commitFirst(Frame first) throws CircuitException {
		if(state<1) {
			throw new CircuitException("503", "没有创建first");
		}
		MemoryInputChannel inpack=new MemoryInputChannel();
		Frame pack=new Frame(inpack,"frame / gateway/1.0");
		pack.content().accept(new MemoryContentReciever());
		inpack.begin(pack);
		byte[] b=first.toBytes();
		inpack.done(b,0,b.length);
		
		onCommitFirstPack(pack);
		state++;
	}

	protected abstract void onCommitFirstPack(Frame pack) throws CircuitException;

	@Override
	public final IInputChannel createContent() throws CircuitException {
		if(state<2) {
			throw new CircuitException("503", "没有提交first");
		}
		MemoryInputChannel incnt=new MemoryInputChannel(8192);
		Frame cnt=new Frame(incnt,"content / gateway/1.0");
		cnt.content().accept(new MemoryContentReciever());
		incnt.begin(cnt);
		state++;
		return incnt;
	}

	@Override
	public final void commitContent(IInputChannel in) throws CircuitException {
		if(state<3) {
			throw new CircuitException("503", "没有创建content");
		}
		in.done(new byte[0], 0, 0);
		onCommitContentPack(in.frame());
		state++;
	}

	protected abstract void onCommitContentPack(Frame cnt)throws CircuitException;

	@Override
	public final IInputChannel createLast() throws CircuitException {
		if(state<2) {
			throw new CircuitException("503", "没有提交first");
		}
		MemoryInputChannel inlast=new MemoryInputChannel(8192);
		Frame last=new Frame(inlast,"last / gateway/1.0");
		last.content().accept(new MemoryContentReciever());
		inlast.begin(last);
		state++;
		return inlast;
	}

	@Override
	public final void commitLast(IInputChannel in) throws CircuitException {
		if(state<3) {
			throw new CircuitException("503", "没有创建last");
		}
		in.done(new byte[0], 0, 0);
		onCommitLastPack(in.frame());
		state=0;
	}

	protected abstract void onCommitLastPack(Frame last)throws CircuitException;
}
