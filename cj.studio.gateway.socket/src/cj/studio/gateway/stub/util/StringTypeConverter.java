package cj.studio.gateway.stub.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import cj.ultimate.gson2.com.google.gson.Gson;

public interface StringTypeConverter {
	@SuppressWarnings("unchecked")
	default <T> T convertFrom(Class<T> clazz, String src) {
		if (clazz.equals(String.class)) {
			return (T) src;
		}
		// namely boolean, byte, char, short, int, long, float, and double.
		if (clazz.isPrimitive()) {
			if (clazz.equals(int.class)) {
				return (T)Integer.valueOf(src);
			}
			if (clazz.equals(boolean.class)) {
				return (T)Boolean.valueOf(src);
			}
			if (clazz.equals(short.class)) {
				return (T)Short.valueOf(src);
			}
			if (clazz.equals(boolean.class)) {
				return (T)Boolean.valueOf(src);
			}
			if (clazz.equals(long.class)) {
				return (T)Long.valueOf(src);
			}
			if (clazz.equals(float.class)) {
				return (T)Float.valueOf(src);
			}
			if (clazz.equals(double.class)) {
				return (T)Double.valueOf(src);
			}
			if (clazz.equals(char.class)) {
				return (T)Character.valueOf(src.charAt(0));
			}
		}
		Method m = null;
		try {
			m = clazz.getMethod("valueOf", String.class);
		} catch (NoSuchMethodException | SecurityException e) {
		}
		if (m != null) {
			m.setAccessible(true);
			try {
				return (T) m.invoke(null, src);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			}
		}
		// 以下使用json
		T o = new Gson().fromJson(src, clazz);
		return o;
	}
}
