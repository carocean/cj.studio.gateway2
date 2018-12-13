package cj.studio.gateway.socket.io.decoder.mutipart;

public enum FormDecodeStep {
	begin, jumpCRLF, expectCRLF, continueCRLF, ifEndField, endField, expectTwoHyphen, ifEndForm, end
}
