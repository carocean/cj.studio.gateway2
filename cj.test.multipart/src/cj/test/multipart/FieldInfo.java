package cj.test.multipart;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cj.ultimate.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class FieldInfo implements IFieldInfo {
	static Pattern fielname = Pattern.compile("filename\\s*=\\s*\"([.[^;=]]*)\"");
	static Pattern name = Pattern.compile("name\\s*=\\s*\"([.[^;=]]*)\"");
	static Pattern bound = Pattern.compile("boundary\\s*=\\s*([.[^;=]]*)");

	List<IAttributeInfo> attributes;
	private IFormData childForm;
	private IFormData owner;
	private ByteBuf valueData;
	private String value;

	public FieldInfo() {
		attributes = new ArrayList<>();
		valueData = Unpooled.buffer();
	}

	@Override
	public String value() {
		if (!StringUtil.isEmpty(value)) {
			return value;
		}
		if (isFile()) {
			return "";
		}
		byte[] b = new byte[valueData.readableBytes()];
		valueData.getBytes(0, b);
		try {
			value = new String(b, "utf-8");
		} catch (UnsupportedEncodingException e) {
		}
		return value;
	}

	@Override
	public IFormData getChildForm() {
		return childForm;
	}

	@Override
	public void addAttribute(IAttributeInfo attribute) {
		attributes.add(attribute);
	}

	@Override
	public void removeAttribute(IAttributeInfo attribute) {
		attributes.remove(attribute);
	}

	@Override
	public int count() {
		return attributes.size();
	}

	@Override
	public boolean isEmpty() {
		return attributes.isEmpty();
	}

	@Override
	public IAttributeInfo get(int index) {
		return attributes.get(index);
	}

	@Override
	public void done() {
	}

	@Override
	public String name() {
		for (IAttributeInfo attr : attributes) {
			if (attr.getType() != AttributeType.ContentDisposition) {
				continue;
			}
			Matcher m = name.matcher(attr.getInfo());
			if (m.find()) {
				return m.group(1);
			} // 如果不存在name则可能是文件则

			m = fielname.matcher(attr.getInfo());
			if (m.find()) {
				return m.group(1);
			}
		}
		return "";// 从disposition解析
	}

	@Override
	public boolean isMixed() {
		for (IAttributeInfo attr : attributes) {
			if (attr.getType() != AttributeType.ContentType) {
				continue;
			}
			return !StringUtil.isEmpty(attr.getInfo()) && attr.getInfo().contains("multipart/mixed");
		}
		return false;
	}

	@Override
	public String childBoundary() {
		for (IAttributeInfo attr : attributes) {
			if (attr.getType() != AttributeType.ContentType) {
				continue;
			}
			Matcher m = bound.matcher(attr.getInfo());
			if (m.find()) {
				String boundary = m.group(1);
				return boundary;
			}
		}
		return null;
	}

	@Override
	public String filename() {
		for (IAttributeInfo attr : attributes) {
			if (attr.getType() != AttributeType.ContentDisposition) {
				continue;
			}
			Matcher m = fielname.matcher(attr.getInfo());
			if (m.find()) {
				return m.group(1);
			}
		}
		return "";
	}

	@Override
	public boolean isFile() {
		for (IAttributeInfo attr : attributes) {
			if (attr.getType() != AttributeType.ContentDisposition) {
				continue;
			}
			return fielname.matcher(attr.getInfo()).find() && !StringUtil.isEmpty(attr.getInfo());
		}
		return false;
	}

	@Override
	public String contentTransferEncoding() {
		for (IAttributeInfo attr : attributes) {
			if (attr.getType() != AttributeType.ContentTransferEncoding) {
				continue;
			}
			String encoding = attr.getInfo();
			String key = "Content-Transfer-Encoding:";
			return encoding.substring(key.length(), encoding.length()).trim();
		}
		return "";
	}

	@Override
	public void setChildForm(IFormData formData) {
		this.childForm = formData;
	}

	@Override
	public IFormData getOwner() {
		return owner;
	}

	@Override
	public void setOwner(IFormData formData) {
		this.owner = formData;
	}

	@Override
	public void writeValue(byte b) {
		valueData.writeByte(b);
	}
}
