package cj.studio.gateway.socket.visitor.decoder.mutipart;

public interface IFormData {

	void done();

	void addField(IFieldInfo currentField);
	void removeField(String name);
	int count();
	boolean isEmpty();
	String[] enumFieldName();

	void setParentField(IFieldInfo field);
	IFieldInfo getParentField();

	IFieldInfo getFieldInfo(String name);
}
