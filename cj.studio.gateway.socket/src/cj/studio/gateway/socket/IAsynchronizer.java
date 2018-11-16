package cj.studio.gateway.socket;

public interface IAsynchronizer {
	void accept(IChunkVisitor visitor);
}
