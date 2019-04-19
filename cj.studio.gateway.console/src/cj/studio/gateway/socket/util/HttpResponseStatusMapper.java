package cj.studio.gateway.socket.util;

import java.util.HashMap;
import java.util.Map;

public class HttpResponseStatusMapper {
	static Map<Integer, String> status;

	public static String message(int state) {
		return status.get(state);
	}

	public static boolean containsStateCode(int state) {
		return status.containsKey(state);
	}

	static {
		status = new HashMap<Integer, String>();

		status.put(100, "Continue");
		status.put(101, "Switching Protocols");
		status.put(102, "Processing");
		status.put(200, "OK");
		status.put(201, "Created");
		status.put(202, "Accepted");
		status.put(203, "Non-Authoriative Information");
		status.put(204, "No Content");
		status.put(205, "Reset Content");
		status.put(206, "Partial Content");
		status.put(207, "Multi-Status");
		status.put(300, "Multiple Choices");
		status.put(301, "Moved Permanently");
		status.put(302, "Found");
		status.put(303, "See Other");
		status.put(304, "Not Modified");
		status.put(305, "Use Proxy");
		status.put(306, "(Unused)");
		status.put(307, "Temporary Redirect");
		status.put(400, "Bad Request");
		status.put(401, "Unauthorized");
		status.put(402, "Payment Granted");
		status.put(403, "Forbidden");
		status.put(404, "Something Not Found");//File Not Found
		status.put(405, "Method Not Allowed");
		status.put(406, "Not Acceptable");
		status.put(407, "Proxy Authentication Required");
		status.put(408, "Request Time-out");
		status.put(409, "Conflict");
		status.put(410, "Gone");
		status.put(411, "Length Required");
		status.put(412, "Precondition Failed");
		status.put(413, "Request Entity Too Large");
		status.put(414, "Request-URI Too Large");
		status.put(415, "Unsupported Media Type");
		status.put(416, "Requested range not satisfiable");
		status.put(417, "Expectation Failed");
		status.put(422, "Unprocessable Entity");
		status.put(423, "Locked");
		status.put(424, "Failed Dependency");
		status.put(500, "Internal Server Error");
		status.put(501, "Not Implemented");
		status.put(502, "Bad Gateway");
		status.put(503, "Service Unavailable");
		status.put(504, "Gateway Timeout");
		status.put(505, "HTTP Version Not Supported, Perhaps App Of Gateway Has Error");
		status.put(507, "Insufficient Storage");
	}
}
