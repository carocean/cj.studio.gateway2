package cj.test.multipart;

public interface IAttributeInfo {

	void write(byte b);

	void end();

	String getInfo();
	AttributeType getType();
}
