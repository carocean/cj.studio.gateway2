package cj.studio.gateway.stub.util;

import cj.ultimate.gson2.com.google.gson.Gson;

public interface StringTypeConverter {
	@SuppressWarnings("unchecked")
	default <T> T convertFrom(Class<T> clazz, String src) {
		if (clazz.equals(String.class)) {
			return (T) src;
		}
		// 以下使用json
		T o = new Gson().fromJson(src, clazz);
		return o;
	}
}
