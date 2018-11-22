package cj.test.multipart;

import cj.test.multipart.siever.BeginBoundarySiever;
import cj.test.multipart.siever.ContinueCrlfSiever;
import cj.test.multipart.siever.ExpectCrlfSiever;
import cj.test.multipart.siever.ExpectEndFieldBoundarySiever;
import cj.test.multipart.siever.ExpectTwoHyphenSiever;
import cj.test.multipart.siever.JumpCrlfSiever;

public class HttpFormDecoder implements IHttpFormDecoder {
	private String nextStep;
	private ISiever currentSiever;
	private IBucket bucket;
	String boundary;

	public HttpFormDecoder(String boundary) {
		this.boundary = boundary;
		bucket = new Bucket();
	}
	@Override
	public void write(byte b) {
		if ("end".equals(nextStep))
			return;
		if(null==nextStep||"".equals(nextStep)) {
			nextStep = "begin";
			selectNext(1);
		}
		if (currentSiever == null) {
			selectNext(1);
		}
		currentSiever.write(b, bucket);
		int end = currentSiever.end();
		if (end > 0) {
			byte[] prevCache = currentSiever.cache();
			boolean hasCache = currentSiever.hasCache();
			selectNext(end);
			checkHasCache(hasCache, prevCache);
		}
	}

	private void checkHasCache(boolean hasCache, byte[] prevCache) {
		if (hasCache) {
			byte[] buf = prevCache;
			for (int i = 0; i < buf.length; i++) {
				write(buf[i]);
			}
		}
	}

	private void selectNext(int prevEnd) {
		switch (nextStep) {
		case "begin":
			bucket.beginForm();
			currentSiever = new BeginBoundarySiever(this.boundary);
			nextStep = "jumpCRLF";
			break;
		case "jumpCRLF":
			bucket.beginField();
			currentSiever = new JumpCrlfSiever();
			nextStep = "expectCRLF";
			break;
		case "expectCRLF":
			bucket.beginAttributeInfo();
			currentSiever = new ExpectCrlfSiever();
			nextStep = "continueCRLF";
			break;
		case "continueCRLF":
			currentSiever = new ContinueCrlfSiever();
			nextStep = "ifEndField";
			break;
		case "ifEndField":
			currentSiever = null;
			bucket.doneAttributeInfo();
			if (prevEnd == 1) {
				bucket.beginFieldData();
				nextStep = "endField";
			} else if (prevEnd == 2) {
				nextStep = "expectCRLF";
			}
			break;
		case "endField":
			currentSiever = new ExpectEndFieldBoundarySiever(this.boundary);
			nextStep = "expectTwoHyphen";
			break;
		case "expectTwoHyphen":
			currentSiever = new ExpectTwoHyphenSiever();
			nextStep = "ifEndForm";
			break;
		case "ifEndForm":
			currentSiever = null;
			bucket.doneFieldData();
			bucket.doneField();
			if (prevEnd == 1) {
				nextStep = "end";
			} else if (prevEnd == 2) {
				bucket.beginField();
				nextStep = "expectCRLF";
			}
			break;
		case "end":
			// 什么也不做，这样就可以把最后一对\r\n丢弃了
			bucket.doneForm();
			break;
		}
	}

}
