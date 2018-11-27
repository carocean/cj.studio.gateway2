package cj.studio.orm.mybatis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.ibatis.session.TransactionIsolationLevel;

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
	/**
	 * 强制connection提交或回滚，默认false
	 * @return
	 */
	boolean force()default false;
	/**
	 * 事务级别，默认READ_COMMITTED
	 * @return
	 */
	TransactionIsolationLevel level() default TransactionIsolationLevel.READ_COMMITTED;
	
}
