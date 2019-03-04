package cj.studio.gateway.socket.io;

import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.socket.pipeline.IOutputer;

public class DefaultFeedbackToOutputer extends AbstractFeedback implements IFeedback {
	IOutputer out;
	Circuit circuit;

	public DefaultFeedbackToOutputer(IOutputer out) {
		this.out = out;
	}

	@Override
	protected void onCommitFirstPack(Frame pack) throws CircuitException {
		circuit = new Circuit(String.format("%s 200 OK", pack.protocol()));
		out.send(pack, circuit);
	}

	@Override
	protected void onCommitContentPack(Frame cnt) throws CircuitException {
		out.send(cnt, circuit);
	}

	@Override
	protected void onCommitLastPack(Frame last) throws CircuitException {
		out.send(last, circuit);
		circuit = null;
	}

}
