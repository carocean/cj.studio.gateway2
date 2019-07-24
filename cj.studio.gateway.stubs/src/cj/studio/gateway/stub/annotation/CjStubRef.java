package cj.studio.gateway.stub.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 引用远程Rest服务<br>
 * <pre>
 * - 存根调用优点是显式的api
 * - 缺点是存根方式api不支持流式续传.
 * 
 * 由于api前后端一致，一端操input，一端reciever，因此两面性的api会给开发带来麻烦，故而存根方式不支持流式续传，如果想用此功能请直接使用frame，circuit的api
 * 
 * </pre>
 * @author caroceanjofers
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface CjStubRef {

	Class<?> stub();
	String remote();
	/**
	 * 是否是异步调用存根。true为异步。<br>
	 * -同步调用存根仅支持http协议，异步调用支持tcp|udt|ws协议
	 * <br>
	 * -异步调用方法无返回值，或永远返回null
	 * <br>
	 * <b>默认为同步调用</b>
	 * @return
	 */
	boolean async() default false;
}
