package cj.studio.gateway.socket.visitor.decoder.mutipart;

public interface IFieldInfo {
	void addAttribute(IAttributeInfo attribute);
	void removeAttribute(IAttributeInfo attribute);
	int count();
	boolean isEmpty();
	IAttributeInfo get(int index);
	String name();
	void done();
	boolean isMixed();
	String childBoundary();
	boolean isFile();
	void setChildForm(IFormData formData);
	IFormData getChildForm();
	void setOwner(IFormData formData);
	IFormData getOwner();
	String filename();
	String contentTransferEncoding();
	void writeValue(byte b);
	String value();
}
