package cj.studio.ecm.net;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.ecm.net.io.MemoryInputChannel;
import cj.ultimate.IDisposable;
import cj.ultimate.gson2.com.google.gson.Gson;
import cj.ultimate.gson2.com.google.gson.JsonElement;
import cj.ultimate.gson2.com.google.gson.JsonObject;
import cj.ultimate.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * graph中传输的侦
 * 
 * <pre>
 * 它包含：字节、参数和头信息，但不能在sink间传递属性引用，如果需要可使用plug.circirl回路
 * 还有：网络中传输的也是侦，不必考虑侦在网络数据流中的消息边界，这交由netty完成分隔
 * </pre>
 * 
 * @author carocean
 *
 */
public class Frame implements IPrinter, IDisposable {
	protected Map<String, String> headmap;
	protected Map<String, String> parametermap;
	protected transient IFrameContent content;
	static transient final String CODE = "utf-8";
	static transient String QUERY_STRING_REG = "(^|\\?|&)\\s*%s\\s*=\\s*([^&]*)(&|$)";

	@Override
	public void dispose() {
		headmap.clear();
		parametermap.clear();
	}

	Frame() {

	}

	public Frame(String frame_line) {
		this(new MemoryInputChannel(8192), frame_line);
	}

	public Frame(byte[] frameRaw) throws CircuitException {
		this(new MemoryInputChannel(8192), frameRaw);
	}

	/**
	 * 传入侦头
	 * 
	 * <pre>
	 * 如：
	 *  GET /PATH?param=22 HTTP/1.1
	 *  其中querystring就是侦参数的简写形式，它会转变到侦参数中
	 * 
	 * </pre>
	 * 
	 * @param frame_line
	 */
	public Frame(IInputChannel input, String frame_line) {
		init(input);

		String[] arr = frame_line.split(" ");// 这种方法如果地址参数中含有空格，则会解析错误，因此将来应改为正则
		if (arr.length < 3)
			throw new RuntimeException("侦头行格式错");
		String cmd = arr[0];
		String pro = arr[arr.length - 1];
		if (!pro.contains("/") || pro.indexOf("/") == pro.length() - 1) {
			throw new RuntimeException("侦没指定协议");
		}
		String mid = frame_line.substring(cmd.length(), frame_line.length() - pro.length()).trim();
		if (StringUtil.isEmpty(mid))
			throw new RuntimeException("侦路径错,如果没有路径，至少指定一个/号");
		arr = new String[] { cmd, mid, pro };
		head("command", arr[0]);
		String uri = arr[1];
		head("url", uri);
		head("protocol", arr[2].toUpperCase());
	}

	Frame(IInputChannel writer) {
		init(writer);
	}
	/**
	 * 该构造没有内容接收器(注：如果frameRaw中的内容则丢失）
	 * @param input
	 * @param frameRaw
	 * @throws CircuitException
	 */
	public Frame(IInputChannel input, byte[] frameRaw) throws CircuitException {
		this(input, null, frameRaw);
	}

	/**
	 * 通过侦数据构造侦
	 * 
	 * <pre>
	 * heads CRLF
	 * CRLF
	 * params CRLF
	 * CRLF
	 * content
	 * </pre>
	 * 
	 * @param frameRaw
	 */
	public Frame(IInputChannel input, IContentReciever reciever, byte[] frameRaw) throws CircuitException {
		init(input);
		if (reciever != null) {
			content.accept(reciever);
		}
		int up = 0;
		int down = 0;
		byte field = 0;// 0=heads;1=params;2=content

		while (down < frameRaw.length) {
			if (field < 2) {// 修改了当内容的头几行是连续空行的情况的bug因此使用了field<2
				if (frameRaw[up] == '\r' && (up + 1 < frameRaw.length && frameRaw[up + 1] == '\n')) {// 跳域
					field++;
					up += 2;
					down += 2;
					continue;
				}
			} else {
				down = frameRaw.length;// 非常变态，bytebuf数组总是在结尾入多一个0，因此其长度总是比写入的长度多1个字节
				byte[] b = new byte[down - up];
				System.arraycopy(frameRaw, up, b, 0, b.length);
				input.writeBytes(b, 0, b.length);
				break;
			}
			if (frameRaw[down] == '\r' && (down + 1 < frameRaw.length && frameRaw[down + 1] == '\n')) {// 跳行
				byte[] b = new byte[down - up];
				System.arraycopy(frameRaw, up, b, 0, b.length);
				try {
					switch (field) {
					case 0:
						String kv = new String(b, CODE);
						int at = kv.indexOf("=");
						String k = kv.substring(0, at);
						String v = kv.substring(at + 1, kv.length());
						if ("protocol".equals(k)) {
							if (v != null)
								v = v.toUpperCase();
						}
						headmap.put(k, v);
						// if ("url".equals(k)
						// && !StringUtil.isEmpty(queryString())) {
						// String[] pair = queryString().split("&");
						// for (String a : pair) {
						// String[] t = a.split("=");
						// String s = t.length > 1 ? t[1] : null;
						// parametermap.put(t[0], s);
						// }
						// }
						break;
					case 1:
						kv = new String(b, CODE);
						at = kv.indexOf("=");
						k = kv.substring(0, at);
						v = kv.substring(at + 1, kv.length());
						parametermap.put(k, "".equals(v) ? null : v);
						break;
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				down += 2;
				up = down;
				continue;
			}
			down++;
		}
		input.done(new byte[0], 0, 0);
	}

	public <T extends Frame> T fillToFrame(Class<T> ft) throws CircuitException {
		try {
			Constructor<T> c = ft.getConstructor(byte[].class);
			byte[] b = toBytes();
			T t = c.newInstance(b);
			return t;
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
			throw new CircuitException("601", e);
		}
	}

	private void init(IInputChannel writer) {
		headmap = new HashMap<String, String>(8);
		parametermap = new HashMap<String, String>(4);
		content = createContent(writer);
	}

	protected FrameContent createContent(IInputChannel input) {
		return new FrameContent(input);
	}

	public synchronized byte[] toBytes() throws CircuitException {
		ByteBuf b = toByteBuf();
		byte[] newArr = new byte[b.readableBytes()];
		b.readBytes(newArr);
		b.release();
		return newArr;
	}

	public String[] enumHeadName() {
		return headmap.keySet().toArray(new String[0]);
	}

	public String contentType() {
		return head("Content-Type");
	}

	/**
	 * min类型
	 * 
	 * <pre>
	 * 如frame/bin,frame/json,others
	 * </pre>
	 * 
	 * @param type
	 */
	public void contentType(String type) {
		head("Content-Type", type);
	}

	public String head(String name) {
		return headmap.get(name);
	}

	public boolean containsHead(String name) {
		return headmap.containsKey(name);
	}

	/**
	 * 判断原始地址(非全地址）中是否存在查询串，即包含?号
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @return
	 */
	public boolean containsQueryString() {
		return headmap.get("url").indexOf("?") >= 0;
	}

	public boolean containsParameter(String key) {
		if (parametermap.containsKey(key))
			return true;
		return containedQueryStrParam(key);
	}

	private boolean containedQueryStrParam(String key) {
		String q = queryString();
		if (StringUtil.isEmpty(q))
			return false;
		// String[] arr = q.split("&");
		// for (String pair : arr) {
		// String[] e = pair.split("=");
		// if (e[0].equals(key)) {
		// return true;
		// }
		// }
		Pattern p = Pattern.compile(String.format(QUERY_STRING_REG, key));
		Matcher m = p.matcher(url());
		if (m.find()) {
			return true;
		}
		return false;
	}

	/**
	 * 设置头信息
	 * 
	 * <pre>
	 * 注：
	 * －有关net回路的请求头设置，请使用NetConstans常量
	 * </pre>
	 * 
	 * @param key
	 * @param v
	 */
	public void head(String key, String v) {
		if (StringUtil.isEmpty(v))
			return;
		if ((key.contains("\r") || key.contains("\n"))) {
			throw new EcmException(String.format("key 不能包含\\r 或 \\n. key is %s, value is %s", key, v));
		}
		if ((v.contains("\r") || v.contains("\n"))) {
			throw new EcmException(String.format("value 不能包含\\r 或 \\n. key is %s, value is %s", key, v));
		}
		if ("protocol".equalsIgnoreCase(key)) {
			v = v.toUpperCase();
		}
		// if ("url".equals(key) && v.indexOf("?") > 0) {
		// String qstr = v.substring(v.indexOf("?") + 1, v.length());
		// if (!StringUtil.isEmpty(qstr)) {
		// String[] qarr = qstr.split("&");
		// for (String kv : qarr) {
		// String k = "";
		// String value = "";
		// if (kv.contains("=")) {
		// int pos = kv.indexOf("=");
		// k = kv.substring(0, pos).trim();
		// value = kv.substring(pos + 1, kv.length());
		// parametermap.put(k, value);
		// }
		// }
		// }
		// String url = v.substring(0, v.indexOf("?"));
		// headmap.put("url", url);
		// return;
		// }
		headmap.put(key, v);
	}

	public void removeHead(String key) {
		headmap.remove(key);
	}

	public String[] enumParameterName() {
		if (headmap.get("url").indexOf("?") < 0) {
			return parametermap.keySet().toArray(new String[0]);
		}
		List<String> keys = new ArrayList<>();
		String[] arr = queryString().split("&");
		for (String pair : arr) {
			if (StringUtil.isEmpty(pair)) {
				continue;
			}
			String[] e = pair.split("=");
			keys.add(e[0]);
		}
		for (String key : parametermap.keySet()) {
			keys.add(key);
		}
		return keys.toArray(new String[0]);
	}

	public String parameter(String name) {
		if (parametermap.containsKey(name))
			return parametermap.get(name);
		Pattern p = Pattern.compile(String.format(QUERY_STRING_REG, name));
		Matcher m = p.matcher(url());
		if (m.find()) {
			return m.group(2).trim();
		}
		return null;
	}

	/**
	 * 参数
	 * 
	 * <pre>
	 * 注：querystring的参数不能被覆盖，否则报异常
	 * </pre>
	 * 
	 * @param key
	 * @param v
	 */
	public void parameter(String key, String v) {
		if (StringUtil.isEmpty(v))
			return;
		if ((key.contains("\r") || key.contains("\n"))) {
			throw new EcmException("不能包含\\r 或 \\n");
		}
		if (v.contains("\r") || v.contains("\n")) {
			throw new RuntimeException("不能包含\\r 或 \\n");
		}
		if (containedQueryStrParam(key)) {
			throw new RuntimeException("不可覆盖querystring参数." + key);
		}
		parametermap.put(key, v);
	}

	/*
	 * { "head":{"key1":"v1","key2":"v2"}, "para":{"key1":"v1","key2":"v2"},
	 * "content":"" }
	 */
	public String toJson() throws CircuitException {
		StringBuffer sb = new StringBuffer();
		sb.append("{");
		if (headmap.isEmpty()) {
			sb.append("\"headers\":{}");
		} else {
			sb.append("\"headers\":{");
		}
		for (String key : headmap.keySet()) {
			String v = headmap.get(key);
			if (StringUtil.isEmpty(v)) {
				v = "";
			}
			sb.append(String.format("\"%s\":\"%s\",", key, v));
		}
		if (!headmap.isEmpty()) {
			sb.append("#$#}");
		}
		if (parametermap.isEmpty()) {
			sb.append(",\"parameters\":{}");
		} else {
			sb.append(",\"parameters\":{");
		}
		for (String key : parametermap.keySet()) {
			String v = parametermap.get(key);
			if (StringUtil.isEmpty(v)) {
				v = "";
			}
			sb.append(String.format("\"%s\":\"%s\",", key, v));
		}
		if (!parametermap.isEmpty()) {
			sb.append("#$#}");
		}
		if (this.content != null && this.content.revcievedBytes() > 0) {
			FrameContent cnt = (FrameContent) content;
			IContentReciever cr = cnt.reciever;
			if (!(cr instanceof MemoryContentReciever)) {
				throw new CircuitException("500", "该方法只有在的内容接收器是MemoryContentReciever时才可用");
			}
			MemoryContentReciever mcr = (MemoryContentReciever) cr;
			byte[] data = mcr.readFully();
			sb.append(String.format(",\"content\":\"%s\"}", new String(data).replace("\"", "\\\"")));
		} else {
			sb.append("}");
		}
		return sb.toString().replace(",#$#", "");
	}

	public static Frame createFrame(String json) throws CircuitException {
		Frame f = new Frame();
		f.fromJson(json);
		return f;
	}

	public static Frame createFrame(String json, Class<? extends Frame> type) throws CircuitException {
		try {
			Object o = type.newInstance();
			Frame f = (Frame) o;
			f.fromJson(json);
			return f;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new EcmException(e);
		}
	}

	public void fromJson(String text) throws CircuitException {
		Gson gson = new Gson();
		JsonElement e = gson.fromJson(text, JsonElement.class);
		JsonObject f = e.getAsJsonObject();
		JsonElement heade = f.get("headers");
		if (heade != null) {
			if (headmap == null) {
				headmap = new HashMap<>();
			}
			JsonObject head = heade.getAsJsonObject();
			for (Map.Entry<String, JsonElement> en : head.entrySet()) {
				headmap.put(en.getKey(), en.getValue() == null ? "" : en.getValue().getAsString());
			}
		}
		JsonElement parae = f.get("parameters");
		if (parae != null) {
			if (parametermap == null) {
				parametermap = new HashMap<>();
			}
			JsonObject para = parae.getAsJsonObject();
			for (Map.Entry<String, JsonElement> en : para.entrySet()) {
				parametermap.put(en.getKey(), en.getValue() == null ? "" : en.getValue().getAsString());
			}
		}
		JsonElement conte = f.get("content");
		if (conte != null) {
			if (content != null) {
				byte[] b = conte.getAsString().getBytes();
				((FrameContent) content).input.done(b, 0, b.length);
			} else {
				MemoryInputChannel input = new MemoryInputChannel();
				content = new FrameContent(input);
				content.accept(new MemoryContentReciever());
				input.begin(this);
				byte[] b = conte.getAsString().getBytes();
				input.done(b, 0, b.length);
			}
		}
	}

	public void removeParameter(String key) {
		parametermap.remove(key);
	}

	/**
	 * 协议均为大写
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @return
	 */
	public String protocol() {
		return head("protocol");
	}

	/**
	 * 协议
	 * 
	 * <pre>
	 * 输入将转换为大写
	 * </pre>
	 * 
	 * @param protocol
	 */
	public void protocol(String protocol) {
		head("protocol", protocol.toUpperCase());
	}

	public IFrameContent content() {
		return content;
	}

	/**
	 * 返回原地址（包括查询串）
	 * 
	 * <pre>
	 * 原地址是构造侦时的初始地址(即便是向侦添加了参数，原地址也不变)
	 * </pre>
	 * 
	 * @return
	 */
	public String url() {
		return headmap.get("url");
	}

	/**
	 * 根路径名，如果url=/则根路径为""
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @return
	 */
	public String rootName() {
		String root = rootPath();
		if (root.equals("/")) {
			return "";
		} else {
			return root.substring(1, root.length());
		}
	}

	/**
	 * 不带root路径的路径，不包含查询串
	 */
	public String relativePath() {
		String path = path();
		path = path.substring(rootPath().length(), path.length());
		if (!path.startsWith("/")) {
			path = String.format("/%s", path);
		}
		return path;
	}

	/**
	 * 不带root的原地址，包含查询串
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @return
	 */
	public String relativeUrl() {
		String rurl = url();
		rurl = rurl.substring(rootPath().length(), rurl.length());
		if (!rurl.startsWith("/")) {
			rurl = String.format("/%s", rurl);
		}
		return rurl;
	}

	/**
	 * url中的根路径
	 * 
	 * <pre>
	 * 如果url=/则root根为/
	 * 否则root即为第一个/与第二个/（如果有）之间，且root值包括第一个/
	 * </pre>
	 * 
	 * @return
	 */
	public String rootPath() {
		String path = path();
		if ("/".equals(path))
			return path;
		path = path.startsWith("/") ? path : String.format("/%s", path);

		int nextSp = path.indexOf("/", 1);
		if (nextSp < 0) {
			if (path.indexOf(".") >= 0) {
				return "/";
			} else {
				return path;
			}
		}
		path = path.substring(0, nextSp);
		return path;
	}

	/**
	 * 带root路径的地址，无查询串
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @return
	 */
	public String path() {
		String p = null;
		String url = url();

		if (url.contains("?")) {
			// String arr[] = url.split("\\?");
			p = url.substring(0, url.indexOf("?"));
		} else {
			p = url;
		}
		return p;
	}

	/**
	 * 请求的文件名，不含路径，但含扩展名，如：/dir1/dir2.exe，其name为dir2.exe
	 * 
	 * <pre>
	 * 注意：如果文件名中不包含.号，视为目录，当是目录时返回的文件名为空串
	 * </pre>
	 * 
	 * @return
	 */
	public String name() {
		String p = path();
		if (p.endsWith("/"))
			return "";
		p = p.substring(p.lastIndexOf("/") + 1, p.length());
		if (!p.contains(".")) {
			return "";
		}
		return p;
	}

	/**
	 * 返回原地址的查询串
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @return
	 */
	public String queryString() {
		String q = "";
		String url = url();
		if (url.contains("?")) {
			// String[] arr = url.split("\\?");
			// if (arr.length > 1) {
			// q = arr[1];
			// }
			q = url.substring(url.indexOf("?") + 1, url.length());
		}
		return q;
	}

	/**
	 * 找回路径
	 * 
	 * <pre>
	 * 即：将所有参数拼到地址后面
	 * </pre>
	 * 
	 * @return
	 */
	public String retrieveUrl() {
		String q = retrieveQueryString();
		if (StringUtil.isEmpty(q)) {
			return url();
		}
		String url = String.format("%s?%s", path(), q);
		return url;
	}

	/**
	 * 找回查询串
	 * 
	 * <pre>
	 * 即：将所有参数拼到原查询串后面
	 * </pre>
	 * 
	 * @return
	 */
	public String retrieveQueryString() {
		String q = queryString();
		if (!StringUtil.isEmpty(q) && q.endsWith("&")) {
			q = q.substring(0, q.length() - 1);
		}
		Set<String> set = parametermap.keySet();
		for (String key : set) {
			String v = parametermap.get(key);

			q = String.format("%s&%s=%s", q, key, v);

		}
		if (q.startsWith("&")) {
			q = q.substring(1, q.length());
		}
		return q;
	}

	/**
	 * 找回路径
	 * 
	 * <pre>
	 * 即：将所有参数拼到地址后面
	 * </pre>
	 * 
	 * @return
	 */
	public String retrieveUrlAndEncode(String charset) {
		String q = retrieveQueryStringAndEncode(charset);
		if (StringUtil.isEmpty(q)) {
			return url();
		}
		String url = String.format("%s?%s", path(), q);
		return url;
	}

	/**
	 * 找回查询串
	 * 
	 * <pre>
	 * 即：将所有参数拼到原查询串后面
	 * </pre>
	 * 
	 * @return
	 */
	public String retrieveQueryStringAndEncode(String charset) {
		String q = queryString();
		if (!StringUtil.isEmpty(q) && q.endsWith("&")) {
			q = q.substring(0, q.length() - 1);
		}
		Set<String> set = parametermap.keySet();
		for (String key : set) {
			String v = parametermap.get(key);
			try {
				v = URLEncoder.encode(v, charset);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			q = String.format("%s&%s=%s", q, key, v);

		}
		if (q.startsWith("&")) {
			q = q.substring(1, q.length());
		}
		return q;
	}

	/**
	 * 找回路径
	 * 
	 * <pre>
	 * 即：将所有参数拼到地址后面
	 * </pre>
	 * 
	 * @return
	 */
	public String deepRetrieveUrlAndEncode(String charset) {
		String q = deepRetrieveQueryStringAndEncode(charset);
		if (StringUtil.isEmpty(q)) {
			return url();
		}
		String url = String.format("%s?%s", path(), q);
		return url;
	}

	/**
	 * 找回查询串 <br>
	 * 将深度查找QueryString。即除了在参数集合中找，还分析地址中带的参数并编码
	 * 
	 * <pre>
	 * 即：将所有参数拼到原查询串后面
	 * </pre>
	 * 
	 * @return
	 */
	public String deepRetrieveQueryStringAndEncode(String charset) {
		String q = queryString();
		if (!StringUtil.isEmpty(q) && q.endsWith("&")) {
			q = q.substring(0, q.length() - 1);
		}
		if (!StringUtil.isEmpty(q)) {
			String arr[] = q.split("&");
			String nq = "";
			for (String kv : arr) {
				if (StringUtil.isEmpty(kv))
					continue;
				int pos = kv.indexOf("=");
				if (pos < 0)
					continue;
				String k = kv.substring(0, pos);
				String v = kv.substring(pos + 1, kv.length());
				if (!StringUtil.isEmpty(v)) {
					try {
						v = URLEncoder.encode(v, charset);
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
				nq = String.format("%s&%s=%s", nq, k, v);
			}
			q = nq;
		}
		Set<String> set = parametermap.keySet();
		for (String key : set) {
			String v = parametermap.get(key);
			try {
				v = URLEncoder.encode(v, charset);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			q = String.format("%s&%s=%s", q, key, v);

		}
		if (q.startsWith("&")) {
			q = q.substring(1, q.length());
		}
		return q;
	}

	public String command() {
		return head("command").trim();
	}

	public void command(String cmd) {
		head("command", cmd.trim());
	}

	public String contentChartset() {
		return head("content-chartset");
	}

	public void contentChartset(String chartset) {
		head("content-chartset", chartset);
	}

	@Override
	public String toString() {
		return String.format("%s %s %s", command(), url(), protocol());
	}

	/**
	 * 判断url路么是否是目录
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @return
	 */
	public boolean isDirectory() {
		String path = path();
		String fn = path.substring(path.lastIndexOf("/") + 1, path.length());
		return !fn.contains(".");
	}

	/**
	 * 获取扩展名，如果没有则返回空串""
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @return
	 */
	public String extName() {
		String ruri = relativePath();
		Pattern pat = Pattern.compile("\\w+\\.(\\w+)$");
		Matcher m = pat.matcher(ruri);
		boolean isfile = m.find();
		if (isfile) {
			return m.group(1);
		}
		return "";
	}

	/**
	 */
	@Override
	public void print(StringBuffer sb) throws CircuitException {
		print(sb, null);
	}

	@Override
	public void print(StringBuffer sb, String indent) throws CircuitException {
		if (sb == null)
			return;
		sb.append(new String(toBytes()));
	}

	public ByteBuf toByteBuf() throws CircuitException {
		ByteBuf b = Unpooled.buffer();
		byte[] crcf = null;
		try {
			crcf = "\r\n".getBytes(CODE);
		} catch (UnsupportedEncodingException e) {
			throw new EcmException(e);
		}
		if (!headmap.containsKey("Content-Length")) {
			long len = content.revcievedBytes();
			headmap.put("Content-Length", Long.toString(len));
		}
		for (String key : headmap.keySet()) {
			String v = headmap.get(key);
			if (StringUtil.isEmpty(v)) {
				continue;
			}
			String tow = key + "=" + v + "\r\n";
			try {
				b.writeBytes(tow.getBytes(CODE));
			} catch (UnsupportedEncodingException e) {
				throw new EcmException(e);
			}
		}
		b.writeBytes(crcf);
		for (String key : parametermap.keySet()) {
			String v = parametermap.get(key);
			if (/* StringUtil.isEmpty(v) || */containedQueryStrParam(key)) {
				continue;
			}
			String tow = key + "=" + (StringUtil.isEmpty(v) ? "" : v) + "\r\n";
			try {
				b.writeBytes(tow.getBytes(CODE));
			} catch (UnsupportedEncodingException e) {
				throw new EcmException(e);
			}
		}
		b.writeBytes(crcf);
		if (this.content.revcievedBytes() > 0) {
			FrameContent cnt = (FrameContent) content;
			IContentReciever cr = cnt.reciever;
			if (cr instanceof MemoryContentReciever) {
				MemoryContentReciever mcr = (MemoryContentReciever) cr;
				byte[] data = mcr.readFully();
				b.writeBytes(data);
			}
		}
		return b;
	}

	public void url(String url) {
		head("url", url);
	}

}
