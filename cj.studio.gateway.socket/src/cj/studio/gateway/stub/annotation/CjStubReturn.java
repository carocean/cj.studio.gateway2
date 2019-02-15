package cj.studio.gateway.stub.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface CjStubReturn {

	String usage();
	/**
	 * 指定返回的具体类型，不指定则系统使用方法返回类型
	 * @return
	 */
	Class<?> type() default Void.class;
	/**
	 * 如果返回的是集合，此处声明元素类型，如果集合缺少元素类型声明运行时会报异常
	 * @return
	 */
	Class<?>[] elementType() default Void.class;
}
