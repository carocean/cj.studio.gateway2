package cj.studio.gateway.mic;

import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.socket.pipeline.IIPipeline;
import cj.studio.gateway.socket.pipeline.IInputValve;

public class LastMicInputValve implements IInputValve {

	@Override
	public void onActive(String inputName, IIPipeline pipeline) throws CircuitException {
		System.out.println("-----onActive");
		pipeline.nextOnActive(inputName, this);
	}

	@Override
	public void flow(Object request, Object response, IIPipeline pipeline) throws CircuitException {
		System.out.println("-----flow:"+request);
		pipeline.nextFlow(request, response, this);
	}

	@Override
	public void onInactive(String inputName, IIPipeline pipeline) throws CircuitException {
		System.out.println("-----onInactive");
		pipeline.nextOnInactive(inputName, this);
	}


}
