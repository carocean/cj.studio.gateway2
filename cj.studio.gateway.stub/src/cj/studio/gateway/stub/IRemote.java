package cj.studio.gateway.stub;

import cj.studio.ecm.net.CircuitException;

public interface IRemote {

	<T>T open(Class<T> stub) throws CircuitException;


}
