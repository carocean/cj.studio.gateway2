package cj.studio.ecm.net;

import cj.studio.ecm.net.io.MemoryContentReciever;

//frame的内容可以完全异步，servlet3.0在httpservletrequest中无内容字段，tomcat,jetty等在该对象外做了异步接收器，各个厂商的实现均不同。
class FrameContent implements IFrameContent {
	IInputChannel input;
	IContentReciever reciever;

	public FrameContent(IInputChannel input) {
		this.input = input;
	}
	@Override
	public boolean canAccept() {
		return reciever==null?true:false;
	}
	@Override
	public void accept(IContentReciever reciever) throws CircuitException {
		if(this.reciever!=null) {
			throw new CircuitException("505", "已存在接收器："+this.reciever.getClass());
		}
		input.accept(reciever);
		this.reciever=reciever;
	}

	@Override
	public long revcievedBytes() throws CircuitException{
		return input.writedBytes();
	}
	@Override
	public boolean isAllInMemory() {
		return reciever instanceof MemoryContentReciever;
	}
	@Override
	public byte[] readFully()throws CircuitException {
		if(!isAllInMemory()) {
			throw new CircuitException("505","不支持readFully方法，因为接收器不是MemoryContentReciever");
		}
		return ((MemoryContentReciever)reciever).readFully();
	}
}
