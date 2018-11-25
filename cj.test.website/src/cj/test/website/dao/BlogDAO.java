package cj.test.website.dao;

import org.apache.ibatis.session.SqlSession;

import cj.studio.ecm.annotation.CjBridge;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.orm.mybatis.IDBEnvironment;
import cj.test.website.bo.BlogBO;

@CjBridge(aspects="transaction")
@CjService(name="blogDAO")
public class BlogDAO implements IBlogDAO {
	@CjServiceRef(refByName="DBEnvironment")
	IDBEnvironment env;
	/* (non-Javadoc)
	 * @see cj.test.website.dao.IBlogDAO#saveDAO(cj.test.website.bo.BlogBO)
	 */
	@Override
	public void saveDAO(BlogBO bo) {
		SqlSession session=env.factory().openSession();
		
	}
}
