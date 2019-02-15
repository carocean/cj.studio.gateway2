package cj.studio.gateway.stub.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;


class ParameterizedTypeImpl implements ParameterizedType {
	Type[] actualTypeArguments;
        Class<?> rawType;
        public ParameterizedTypeImpl(Class<?> rawType,Type[] actualTypeArguments,String onmessage) {
        	this.actualTypeArguments = actualTypeArguments;
            this.rawType=rawType;
            if(Map.class.isAssignableFrom(rawType)) {
        		if(actualTypeArguments==null||actualTypeArguments.length!=2) {
        			throw new RuntimeException("缺少Map及其派生类的元素Key和value的类型声明,在："+onmessage);
        		}
        	}
        	if(Collection.class.isAssignableFrom(rawType)) {
        		if(actualTypeArguments==null||actualTypeArguments.length!=1) {
        			throw new RuntimeException("缺少Collection及其派生类的元素类型声明,在："+onmessage);
        		}
        	}
        }

        @Override
        public Type[] getActualTypeArguments() {
            return actualTypeArguments;
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public Type getOwnerType() {
            return rawType.getDeclaringClass();
        }
    }