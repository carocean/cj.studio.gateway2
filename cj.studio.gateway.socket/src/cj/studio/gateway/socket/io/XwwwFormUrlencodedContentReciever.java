package cj.studio.gateway.socket.io;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IContentReciever;
import cj.studio.ecm.net.util.WebUtil;
import cj.ultimate.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public abstract class XwwwFormUrlencodedContentReciever implements IContentReciever {
	ByteBuf content;
	Frame frame;
	private boolean isX_www_form_urlencoded;

	@Override
	public final void begin(Frame frame) {// Content-Type=application/x-www-form-urlencoded
		if(frame.contentType().indexOf("x-www-form-urlencoded")>0) {
			isX_www_form_urlencoded=true;
		}
		content = Unpooled.buffer();
		this.frame = frame;
	}

	@Override
	public final void recieve(byte[] b, int pos, int length)  throws CircuitException{
		if(!isX_www_form_urlencoded)return;
		content.writeBytes(b, pos, length);
	}

	@Override
	public final void done(byte[] b, int pos, int length)  throws CircuitException{
		if(!isX_www_form_urlencoded)return;
		if (length - pos < 1) {
			return;
		}
		content.writeBytes(b, pos, length);
		
		byte[] fully=new byte[content.readableBytes()];
		content.readBytes(fully,0,fully.length);
		
		String chartset = frame.contentChartset();
		if (StringUtil.isEmpty(chartset)) {
			chartset = "utf-8";
		}
		String text = "";
		try {
			text = new String(fully, chartset);
		} catch (UnsupportedEncodingException e) {
		}
		if (StringUtil.isEmpty(text))
			return;
		Map<String, Object> params = WebUtil.parserParam(text);
		for (String key : params.keySet()) {
			frame.parameter(key, (String) params.get(key));
		}
		content.clear();
		content.release();
		done(frame);
	}
	/**
	 * 完成事件，派生类可实现
	 * @param frame
	 */
	protected abstract void done(Frame frame) throws CircuitException;

}
