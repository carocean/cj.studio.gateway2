package cj.studio.gateway.socket.util;

import io.netty.channel.ChannelId;

public class SocketName {

	public static String name(ChannelId id, String netName) {
		return  String.format("%s@%s",id.asLongText(),netName);
	}

}
