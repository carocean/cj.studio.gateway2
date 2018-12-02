package cj.studio.gateway.socket.visitor.decoder;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import cj.studio.ecm.frame.Circuit;
import cj.studio.ecm.frame.Frame;
import cj.studio.ecm.net.web.WebUtil;
import cj.studio.gateway.socket.visitor.IHttpFormChunkDecoder;
import cj.studio.gateway.socket.visitor.IHttpVisitorWriter;
import cj.ultimate.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class UrlEncodedFormChunkDecoder implements IHttpFormChunkDecoder {
	Frame frame;
	Circuit circuit;
	ByteBuf content;

	public UrlEncodedFormChunkDecoder(Frame frame, Circuit circuit) {
		this.frame = frame;
		this.circuit = circuit;
		this.content = Unpooled.buffer();
	}

	@Override
	public final void done(IHttpVisitorWriter writer) {
		byte[] b = new byte[content.readableBytes()];
		content.readBytes(b);
		String chartset = frame.contentChartset();
		if (StringUtil.isEmpty(chartset)) {
			chartset = "utf-8";
		}
		String text = "";
		try {
			text = new String(b, chartset);
		} catch (UnsupportedEncodingException e) {
		}
		if (StringUtil.isEmpty(text))
			return;
		Map<String,Object>params=WebUtil.parserParam(text);
		for(String key:params.keySet()) {
			frame.parameter(key,(String)params.get(key));
		}
		done(frame,circuit,writer);
		content.clear();
		content.release();
		frame=null;
		circuit=null;
	}

	protected void done(Frame frame, Circuit circuit, IHttpVisitorWriter writer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public final void writeChunk(ByteBuf chunk) {
		content.writeBytes(chunk);
	}

}
