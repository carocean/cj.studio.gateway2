package cj.studio.orm.mybatis.aspect;

import cj.studio.ecm.CJSystem;
import org.apache.ibatis.session.SqlSession;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.bridge.IAspect;
import cj.studio.ecm.bridge.ICutpoint;
import cj.studio.orm.mybatis.MyBatisPlugin;
import cj.studio.orm.mybatis.annotation.CjTransaction;

@CjService(name = "@transaction")
public class DBTransaction implements IAspect {

	@Override
	public Object cut(Object bridge, Object[] args, ICutpoint point) throws Throwable{
		CjTransaction p = point.getMethodAnnotation(CjTransaction.class);
		if (p == null) {
			CJSystem.logging().warn(getClass(),
					String.format("方法缺少事务注解：@CjTransaction, 在：%s.%s。 该方法将在非事务模式下执行" , point.getServiceDefId(), point.getMethodName()));
			return point.cut(bridge, args);
		}
		SqlSession session = MyBatisPlugin.getFactory().getSession(p.level());
		try {
			Object result = point.cut(bridge, args);
//			session.commit(p.force());
			MyBatisPlugin.getFactory().commit(p.force());
			return result;
		} catch (Exception e) {
//			session.rollback(p.force());
			MyBatisPlugin.getFactory().rollback();
			throw e;
		} finally {
			MyBatisPlugin.getFactory().closeSession(session);
		}
	}

	@Override
	public Class<?>[] getCutInterfaces() {
		
		return null;
	}

	@Override
	public void observe(Object arg0) {
		// TODO Auto-generated method stub
		
	}

}
