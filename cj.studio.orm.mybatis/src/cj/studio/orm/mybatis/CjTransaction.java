package cj.studio.orm.mybatis;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注解事务
 * 
 * <pre>
 * 使用该注解需要实现IEntityManagerable接口
 * </pre>
 * 
 * @author carocean
 * @see IEntityManagerable
 */
@Target(value = { ElementType.METHOD, ElementType.CONSTRUCTOR })
@Retention(RetentionPolicy.RUNTIME)
public @interface CjTransaction {
	
}
