package cj.studio.gateway.socket.pipeline;

import cj.studio.ecm.net.CircuitException;

public interface ICloseableOutputValve {
	void close(IOPipeline pipeline)throws CircuitException;
}
