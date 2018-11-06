package cj.studio.gateway.socket.pipeline;

import cj.studio.ecm.graph.CircuitException;

public interface ICloseableOutputValve {
	void close(IOPipeline pipeline)throws CircuitException;
}
