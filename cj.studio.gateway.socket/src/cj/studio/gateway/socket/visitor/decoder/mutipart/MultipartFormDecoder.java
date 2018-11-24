package cj.studio.gateway.socket.visitor.decoder.mutipart;

import cj.studio.gateway.socket.visitor.decoder.mutipart.siever.BeginBoundarySiever;
import cj.studio.gateway.socket.visitor.decoder.mutipart.siever.ContinueCrlfSiever;
import cj.studio.gateway.socket.visitor.decoder.mutipart.siever.ExpectCrlfSiever;
import cj.studio.gateway.socket.visitor.decoder.mutipart.siever.ExpectEndFieldBoundarySiever;
import cj.studio.gateway.socket.visitor.decoder.mutipart.siever.ExpectTwoHyphenSiever;
import cj.studio.gateway.socket.visitor.decoder.mutipart.siever.JumpCrlfSiever;

public class MultipartFormDecoder implements IMultipartFormDecoder {
	private FormDecodeStep nextStep;
	private ISiever currentSiever;
	private IBucket bucket;
	String boundary;

	public MultipartFormDecoder(String boundary, IBucket bucket) {
		this.boundary = boundary;
		this.bucket = bucket;
	}

	@Override
	public void write(byte b) {
		if (nextStep == FormDecodeStep.end)
			return;
		if (null == nextStep) {
			nextStep = FormDecodeStep.begin;
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
		case begin:
			bucket.beginForm();
			currentSiever = new BeginBoundarySiever(this.boundary);
			nextStep = FormDecodeStep.jumpCRLF;
			break;
		case jumpCRLF:
			bucket.beginField();
			currentSiever = new JumpCrlfSiever();
			nextStep = FormDecodeStep.expectCRLF;
			break;
		case expectCRLF:
			bucket.beginAttributeInfo();
			currentSiever = new ExpectCrlfSiever();
			nextStep = FormDecodeStep.continueCRLF;
			break;
		case continueCRLF:
			currentSiever = new ContinueCrlfSiever();
			nextStep = FormDecodeStep.ifEndField;
			break;
		case ifEndField:
			currentSiever = null;
			bucket.doneAttributeInfo();
			if (prevEnd == 1) {
				bucket.beginFieldData();
				nextStep = FormDecodeStep.endField;
			} else if (prevEnd == 2) {
				nextStep = FormDecodeStep.expectCRLF;
			}
			break;
		case endField:
			currentSiever = new ExpectEndFieldBoundarySiever(this.boundary);
			nextStep = FormDecodeStep.expectTwoHyphen;
			break;
		case expectTwoHyphen:
			currentSiever = new ExpectTwoHyphenSiever();
			nextStep = FormDecodeStep.ifEndForm;
			break;
		case ifEndForm:
			currentSiever = null;
			bucket.doneFieldData();
			bucket.doneField();
			if (prevEnd == 1) {
				nextStep = FormDecodeStep.end;
			} else if (prevEnd == 2) {
				bucket.beginField();
				nextStep = FormDecodeStep.expectCRLF;
			}
			break;
		case end:
			// 什么也不做，这样就可以把最后一对\r\n丢弃了
			bucket.doneForm();
			break;
		}
	}

}
