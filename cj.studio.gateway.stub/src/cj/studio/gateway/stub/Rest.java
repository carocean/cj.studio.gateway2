package cj.studio.gateway.stub;

import cj.studio.ecm.adapter.IAdaptable;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.socket.pipeline.IOutputSelector;
import cj.ultimate.net.sf.cglib.proxy.Enhancer;

@CjService(name = "$.rest")
public class Rest implements IRest {
	@CjServiceRef(refByName = "$.output.selector")
	IOutputSelector _selector;

	@Override
	public IRemote forRemote(String remote) throws CircuitException {
		return new Remote(_selector, remote);
	}

	class Remote implements IRemote {
		IOutputSelector _selector;
		String remote;

		public Remote(IOutputSelector _selector, String remote) {
			this._selector = _selector;
			this.remote = remote;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T open(Class<T> stub) throws CircuitException {
			if (!stub.isInterface()) {
				throw new CircuitException("503", "不是接口类型：" + stub);
			}

			// 实现代理
			Enhancer en = new Enhancer();
			en.setClassLoader(stub.getClassLoader());
			en.setSuperclass(Object.class);
			en.setInterfaces(new Class<?>[] { stub, IAdaptable.class });
			en.setCallback(new SyncInvocationHandler(_selector, remote, stub));
			return (T) en.create();
		}

	}

}
