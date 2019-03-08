package cj.studio.ecm.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class DefaultSegmentCircuit extends Circuit {

	public DefaultSegmentCircuit(IOutputChannel output, String frame_line) {
		super(output, frame_line);
	}

	public DefaultSegmentCircuit(IOutputChannel output) {
		super(output);
	}

	public DefaultSegmentCircuit(String frame_line) {
		super(frame_line);
	}

	@Override
	protected ICircuitContent createContent(IOutputChannel output, int capacity) {
		ByteBuf buf = null;
		buf = Unpooled./* directBuffer */buffer(capacity);
		return new DefaultSegmentCircuitContent(this, output, buf, capacity);
	}
}
