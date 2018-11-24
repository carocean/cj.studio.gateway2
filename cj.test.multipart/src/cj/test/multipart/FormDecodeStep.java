package cj.test.multipart;

public enum FormDecodeStep {
	begin, jumpCRLF, expectCRLF, continueCRLF, ifEndField, endField, expectTwoHyphen, ifEndForm, end
}
