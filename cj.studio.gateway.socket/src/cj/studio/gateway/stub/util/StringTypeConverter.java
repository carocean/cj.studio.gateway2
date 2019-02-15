package cj.studio.gateway.stub.util;

import cj.ultimate.gson2.com.google.gson.Gson;

public interface StringTypeConverter {
	@SuppressWarnings("unchecked")
	default <T> T convertFrom(Class<T> rawType,Class<?>[] eleType, String src,String onmessage) {
		if(src==null)return null;
		if (rawType.equals(String.class)) {
			return (T) src;
		}
		if(eleType==null||eleType.equals(Void.class)||rawType.isArray()) {
			// 以下使用json
			T o = new Gson().fromJson(src, rawType);
			return o;
		}
		ParameterizedTypeImpl pti=new ParameterizedTypeImpl(rawType, eleType,onmessage);
		T o = new Gson().fromJson(src, pti);
		return o;
	}
}
