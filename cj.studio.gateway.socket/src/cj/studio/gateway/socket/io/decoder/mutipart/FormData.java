package cj.studio.gateway.socket.io.decoder.mutipart;

import java.util.HashMap;
import java.util.Map;

public class FormData implements IFormData {
	Map<String, IFieldInfo> fields;
	private IFieldInfo parentField;

	public FormData() {
		fields = new HashMap<>();
	}

	@Override
	public void setParentField(IFieldInfo field) {
		field.setChildForm(this);
		this.parentField = field;
	}

	@Override
	public IFieldInfo getParentField() {
		return parentField;
	}

	@Override
	public IFieldInfo getFieldInfo(String name) {
		return fields.get(name);
	}

	public boolean containsField(String name) {
		return fields.containsKey(name);
	}

	@Override
	public void done() {

	}

	@Override
	public void addField(IFieldInfo field) {
		if (field.isFile()) {//一个file元素多个文件时，一个元素的name会对应一个文件集，故而采用hashCode为之命名
			this.fields.put(String.format("%s$%s", field.name(),Math.abs(field.filename().hashCode())), field);
		} else {
			this.fields.put(field.name(), field);
		}
	}

	@Override
	public void removeField(String name) {
		this.fields.remove(name);
	}

	@Override
	public int count() {
		return fields.size();
	}

	@Override
	public boolean isEmpty() {
		return fields.isEmpty();
	}

	@Override
	public String[] enumFieldName() {
		return fields.keySet().toArray(new String[0]);
	}

}
