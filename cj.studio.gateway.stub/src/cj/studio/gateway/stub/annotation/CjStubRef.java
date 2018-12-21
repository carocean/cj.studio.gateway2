package cj.studio.gateway.stub.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 引用远程Rest服务
 * @author caroceanjofers
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface CjStubRef {

	Class<?> stub();

	String remote();
	
	
}
