package cj.studio.gateway.socket.io.decoder.mutipart;

public class Bucket implements IBucket {
	private IAttributeInfo currentAttribute;
	private IFieldInfo currentField;
	private IFormData currentFormData;
	private IFieldDataListener listener;
	private MultipartFormDecoder childDecoder;
	private IFieldInfo parentField;
	private boolean currentIsFile;
	private boolean currentIsMixed;

	public Bucket(IFieldDataListener listener) {
		this.listener = listener;
	}
	
	@Override
	public void setParentField(IFieldInfo field) {
		this.parentField = field;
	}

	@Override
	public void beginForm() {
		currentFormData = new FormData();
		if (parentField != null) {
			currentFormData.setParentField(parentField);
		}
	}
	@Override
	public IFormData getForm() {
		return currentFormData;
	}
	@Override
	public void doneForm() {
		currentFormData.done();
	}

	@Override
	public void beginField() {
		currentField = new FieldInfo();
		currentField.setOwner(currentFormData);
	}

	@Override
	public void doneField() {
		currentField.done();
		currentFormData.addField(currentField);
	}

	@Override
	public void beginFieldData() {
		this.currentIsFile=currentField.isFile();
		this.currentIsMixed=currentField.isMixed();
		if (listener != null) {
			listener.openFD(currentField);
		}
		if (currentIsMixed) {
			IBucket childBucket = new Bucket(listener);
			childBucket.setParentField(currentField);
			this.childDecoder = new MultipartFormDecoder(currentField.childBoundary(), childBucket);
		}
	}

	@Override
	public void doneFieldData() {
		if (listener != null) {
			listener.doneFD();
		}
		if (currentIsMixed) {
			this.childDecoder = null;
		}
		currentIsFile=false;
		currentIsMixed=false;
	}

	@Override
	public void beginAttributeInfo() {
		this.currentAttribute = new AttributeInfo();

	}

	@Override
	public void doneAttributeInfo() {
		currentAttribute.end();
		currentField.addAttribute(currentAttribute);
	}

	@Override
	public void writeAttributeInfo(byte b) {
		currentAttribute.write(b);
	}

	@Override
	public void writeFieldData(byte b) {
		// 如果data流包含有边界，则说明是mixed模式，含有子表单，则新建解码器解码
		if (this.childDecoder != null&&currentIsMixed) {
			childDecoder.write(b);
			return;
		}

		if (listener != null) {
			if(currentIsFile) {
				listener.writeFD(b);
			}else {
				currentField.writeValue(b);
			}
		}
	}

}
