package cj.studio.ecm.net;

public interface IContentWriter {
    void write(IOutputChannel output) throws CircuitException;

    boolean isFinished();

    long length();
}
