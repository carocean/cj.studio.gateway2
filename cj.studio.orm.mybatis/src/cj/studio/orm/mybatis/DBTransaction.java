package cj.studio.orm.mybatis;

import org.apache.ibatis.session.SqlSession;

import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.bridge.IAspect;
import cj.studio.ecm.bridge.ICutpoint;

@CjService(name = "transaction")
public class DBTransaction implements IAspect {
	@CjServiceRef(refByName = "DBEnvironment")
	IDBEnvironment env;

	@Override
	public Object cut(Object bridge, Object[] args, ICutpoint point) {
		CjTransaction p = point.getMethodAnnotation(CjTransaction.class);
		if (p == null) {
			return point.cut(bridge, args);
		}
		SqlSession session = env.factory().openSession();
		try {
			Object result = point.cut(bridge, args);
			session.commit();
			return result;
		} catch (Exception e) {
			session.rollback();
			throw e;
		} finally {
			session.close();
		}
	}

	@Override
	public Class<?>[] getCutInterfaces() {
		// TODO Auto-generated method stub
		return null;
	}

}
