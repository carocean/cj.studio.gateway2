package cj.studio.gateway.stub.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 回路状态消息重置，一般用于http响应状态码匹配设置<br>
 * 由于netty的http响应头仅支持ascii码，造成响应的消息中文乱码，因此需要重置应用中状态码对应的消息为英文
 * 
 * @author caroceanjofers
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface CjStubCircuitStatusMatches {
	/**
	 * 回路响应状态和消息<br>
	 * 消息务必要指定为<b>英文</b>，中文必为乱码，原因见本注解的类型说明
	 * <pre>
	 * 用法：
	 * 	CjStubHttpStatusMatches(httpStatus = "200 ok")
	 * 	CjStubHttpStatusMatches(httpStatus = {"200 ok","201 xxx})
	 * </pre>
	 * @return
	 */
	String[] status() default {};
}
