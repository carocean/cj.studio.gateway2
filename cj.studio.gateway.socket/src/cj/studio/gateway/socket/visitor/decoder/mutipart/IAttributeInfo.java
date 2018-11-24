package cj.studio.gateway.socket.visitor.decoder.mutipart;

public interface IAttributeInfo {

	void write(byte b);

	void end();

	String getInfo();
	AttributeType getType();
}
