package cj.studio.gateway.socket.io.decoder.mutipart;

public interface IBucket {
	
	void writeAttributeInfo(byte b);
	void writeFieldData(byte b);
	
	void beginForm();
	void doneForm();
	
	void beginField();
	void doneField();
	
	void beginAttributeInfo();
	void doneAttributeInfo();
	
	void beginFieldData();
	void doneFieldData();
	/**
	 * 当域为mixed时存在子表单，该方法表示设置子表单所在的mixed域
	 * @param field
	 */
	void setParentField(IFieldInfo field);
	IFormData getForm();
	
	
}
