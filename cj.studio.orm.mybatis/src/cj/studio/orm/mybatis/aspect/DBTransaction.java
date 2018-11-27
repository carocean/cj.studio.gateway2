package cj.studio.orm.mybatis.aspect;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceAfter;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.bridge.IAspect;
import cj.studio.ecm.bridge.ICutpoint;
import cj.studio.orm.mybatis.MyBatisPlugin;
import cj.studio.orm.mybatis.annotation.CjTransaction;

@CjService(name = "transaction")
public class DBTransaction implements IAspect, IServiceAfter {
	SqlSessionFactory factory;

	@Override
	public void onAfter(IServiceSite site) {
		factory = (SqlSessionFactory) site.getService("");

	}

	@Override
	public Object cut(Object bridge, Object[] args, ICutpoint point) {
		CjTransaction p = point.getMethodAnnotation(CjTransaction.class);
		if (p == null) {
			throw new EcmException(
					"方法缺少事务注解：@CjTransaction 在：" + point.getServiceDefId() + "." + point.getMethodName());
		}
		SqlSession session = MyBatisPlugin.getFactory().getSession(p.level());
		try {
			Object result = point.cut(bridge, args);
			session.commit(p.force());
			return result;
		} catch (Exception e) {
			session.rollback(p.force());
			throw e;
		} finally {
			MyBatisPlugin.getFactory().closeSession(session);
		}
	}

	@Override
	public Class<?>[] getCutInterfaces() {
		// TODO Auto-generated method stub
		return null;
	}

}
