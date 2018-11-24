package cj.studio.gateway.socket.visitor.decoder.mutipart;

public enum FormDecodeStep {
	begin, jumpCRLF, expectCRLF, continueCRLF, ifEndField, endField, expectTwoHyphen, ifEndForm, end
}
