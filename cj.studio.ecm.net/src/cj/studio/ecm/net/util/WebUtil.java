package cj.studio.ecm.net.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebUtil {
	
	/**
	 * 
	 * <pre>
	 * 注意：无扩展名的视为目录，目录被视为文档，因为如果是资源必有扩展名
	 * </pre>
	 * 
	 * @param path
	 *            请求的资源的url
	 * @param docType
	 *            格式如：.html|.htm|.app
	 * @return
	 */
	public static synchronized boolean documentMatch(String path,
			String docType) {
		String fn = path.substring(path.lastIndexOf("/") + 1, path.length());
		if (!fn.contains(".")) {
			return true;// 是目录则一定是文档
		}
		if (fn.contains("#")) {
			fn = fn.substring(0, fn.lastIndexOf("#"));
		}
		Pattern p = Pattern
				.compile(String.format("%s$", docType.replace(".", ".*\\.")));
		Matcher m = p.matcher(fn);
		return m.matches();
	}

	/**
	 * 将querystring对解析为参数
	 * 
	 * <pre>
	 * 注意：如果js中提交数组，则接收的key即是key[]=xx&key[]=jj，因此将它直接解析为list对象值
	 * </pre>
	 * 
	 * @param data
	 * @return
	 */
	public synchronized static Map<String, Object> parserParam(String data) {
		
		Map<String, Object> map = new HashMap<>();
		String[] kvpair = data.split("&");
		for (String kvStr : kvpair) {
			String[] kv = kvStr.split("=");
			String k = kv[0];
			String v = kv.length > 1 ? kv[1] : "";
			try {
				k = URLDecoder.decode(k, "utf-8");
				v = URLDecoder.decode(v, "utf-8");
			} catch (UnsupportedEncodingException e) {
			}
			if (k.endsWith("[]")) {
				k = k.substring(0, k.length() - 2);
				@SuppressWarnings("unchecked")
				List<String> list=(List<String>)map.get(k);
				if(list==null){
					list=new ArrayList<>();
					map.put(k, list);
				}
				list.add(v);
			} else {
				map.put(k, v);
			}
		}
		return map;
	}

	public static void main(String... strings) {
		System.out.println(documentMatch("/", ".html|.htm|.app"));
	}
}
