package cj.test.website2.webview;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.stub.annotation.CjStubInContent;
import cj.studio.gateway.stub.annotation.CjStubInHead;
import cj.studio.gateway.stub.annotation.CjStubInParameter;
import cj.studio.gateway.stub.annotation.CjStubMethod;
import cj.studio.gateway.stub.annotation.CjStubReturn;
import cj.studio.gateway.stub.annotation.CjStubService;
import cj.studio.gateway.stub.printer.PrintStubAppSiteWebView;

@CjService(name = "/printer.html")
public class MyPrintStub extends PrintStubAppSiteWebView {

	@Override
	protected void onprintStubMethodArgInContent(CjStubInContent sic,Parameter p , Method m, Frame frame, Circuit circuit,
			IGatewayAppSiteResource resource) {
		circuit.content().writeBytes(String.format("&emsp;&emsp;&emsp;&emsp;content usage:%s<br>", sic.usage()).getBytes());

	}

	@Override
	protected void onprintStubMethodArgInParameter(CjStubInParameter sip,Parameter p , Method m, Frame frame, Circuit circuit,
			IGatewayAppSiteResource resource) {
		circuit.content().writeBytes(
				String.format("&emsp;&emsp;&emsp;&emsp;paramenter key:%s,usage:%s<br>", sip.key(), sip.usage()).getBytes());
	}

	@Override
	protected void onprintStubMethodArgInHead(CjStubInHead sih,Parameter p , Method m, Frame frame, Circuit circuit,
			IGatewayAppSiteResource resource) {
		circuit.content().writeBytes(
				String.format("&emsp;&emsp;&emsp;&emsp;head key:%s,usage:%s<br>", sih.key(), sih.usage()).getBytes());
	}

	@Override
	protected void onprintStubMethodReturn(CjStubReturn ret, Method m, Frame frame, Circuit circuit,
			IGatewayAppSiteResource resource) {
		circuit.content()
				.writeBytes(String.format("&emsp;&emsp;&emsp;&emsp;return type:%s,usage:%s<br>", ret.type(), ret.usage()).getBytes());

	}

	@Override
	protected void onprintStubService(CjStubService ss, Class<?> c, Frame frame, Circuit circuit,
			IGatewayAppSiteResource resource) {
		circuit.content().writeBytes(String.format("stub:%s", c.getName()).getBytes());
		circuit.content().writeBytes(String.format("bindService:%s,usage:%s<br>", ss.bindService(), ss.usage()).getBytes());

	}

	@Override
	protected void onprintStubMethod(CjStubMethod sm, Method m, Frame frame, Circuit circuit,
			IGatewayAppSiteResource resource) {
		circuit.content()
				.writeBytes(String.format("&emsp;&emsp;&emsp;&emsp;name:%s,alias:%s,command:%s,protocol:%s,usage:%s<br>",
						m.getName(), sm.alias(), sm.command(), sm.protocol(), sm.usage()).getBytes());
	}

	@Override
	protected void onprintEnd(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) {
		// TODO Auto-generated method stub
		circuit.content().writeBytes("<br>".getBytes());
	}

	@Override
	protected void onprintStubServiceEnd(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) {
		// TODO Auto-generated method stub
		circuit.content().writeBytes("<br>".getBytes());
	}

	@Override
	protected void onprintMethodEnd(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) {
		// TODO Auto-generated method stub
		circuit.content().writeBytes("<br>".getBytes());
	}

}
