package cj.studio.gateway.socket.io;

import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;

public class DefaultFeedbackToCircuit extends AbstractFeedback implements IFeedback {
	Circuit circuit;

	public DefaultFeedbackToCircuit(Circuit circuit) {
		this.circuit = circuit;
	}

	@Override
	protected void onCommitFirstPack(Frame first) throws CircuitException {
		circuit.content().writeBytes(first.toBytes());
		circuit.content().flush();
	}

	@Override
	protected void onCommitContentPack(Frame cnt) throws CircuitException {
		circuit.content().writeBytes(cnt.toBytes());
		circuit.content().flush();
	}

	@Override
	protected void onCommitLastPack(Frame last) throws CircuitException {
		circuit.content().writeBytes(last.toBytes());
		circuit = null;
	}
}
