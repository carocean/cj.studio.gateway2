package cj.test.multipart;

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
	
	
}
