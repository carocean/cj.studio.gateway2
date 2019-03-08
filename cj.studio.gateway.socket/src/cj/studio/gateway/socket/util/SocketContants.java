package cj.studio.gateway.socket.util;

public interface SocketContants {
	String __pipeline_name = "Pipeline-Name";// 管道名
	String __pipeline_fromWho = "From-Who";
	String __pipeline_fromProtocol = "From-Protocol";
	String __pipeline_builder_frontend_channel="Frontend-Channel";
	String __pipeline_toWho = "To-Who";
	String __pipeline_toProtocol = "To-Protocol";

	String __frame_fromWho = "From-Who";
	String __frame_fromProtocol = "From-Protocol";
	String __frame_fromPipelineName = "From-Pipeline-Name";
	String __frame_gatewayDest = "Gateway-Dest";
	
	String __http_ws_prop_aggregatorFileLengthLimit="Aggregator-Limit";
	String __http_ws_prop_wsPath="wsPath";
	
	String __circuit_chunk_writer="Chunk-Writer";
	String __request_isclose = "Request-Isclose";
	int __pull_chunk_size = 8192;
	int __upload_chunked_cache_size = 8192;//8k
	String __frame_Head_Rest_Command = "Rest-Command";
	String __frame_Head_Rest_Stub_Interface = "Rest-StubFace";
	String __circuit_same_origin_policy="Same-Origin-Policy";//跨域调用
	String __channel_onchannelEvent_notify_dests="OnChannelEvent-Notify-Dests";
	long __channel_write_await_timeout = 10000L;
}
