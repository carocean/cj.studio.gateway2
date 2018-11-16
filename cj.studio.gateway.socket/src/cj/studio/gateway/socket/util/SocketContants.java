package cj.studio.gateway.socket.util;

public interface SocketContants {
	String __pipeline_name = "Pipeline-Name";// 管道名
	String __pipeline_fromWho = "From-Who";
	String __pipeline_fromProtocol = "From-Protocol";
	String __pipeline_toWho = "To-Who";
	String __pipeline_toProtocol = "To-Protocol";

	String __frame_fromWho = "From-Who";
	String __frame_fromProtocol = "From-Protocol";
	String __frame_fromPipelineName = "From-Pipeline-Name";
	String __frame_gatewayDest = "Gateway-Dest";
	
	String __http_ws_prop_aggregatorFileLengthLimit="Aggregator-Limit";
	String __http_ws_prop_wsPath="wsPath";
	
	String __circuit_chunk_visitor="Chunk-Visitor";
}
