package cj.test.website.service;

import java.util.List;

import org.apache.ibatis.session.TransactionIsolationLevel;

import cj.studio.ecm.annotation.CjBridge;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.orm.mybatis.annotation.CjTransaction;
import cj.test.website.bo.UserBO;
import cj.test.website.dao.IUserDAO;
@CjBridge(aspects="transaction")
@CjService(name="userService")
public class UserService implements IUserService {
	
	@CjServiceRef(refByName="mybatis.cj.test.website.dao.IUserDAO")
	IUserDAO userDAO;
	/* (non-Javadoc)
	 * @see cj.test.website.service.IBlogService#saveBlog(cj.test.website.bo.BlogBO)
	 */
	@CjTransaction(force=true,level=TransactionIsolationLevel.REPEATABLE_READ)
	@Override
	public void saveUser(UserBO bo) {
		userDAO.save(bo);
	}
	@CjTransaction
	@Override
	public List<UserBO> query() {
		// TODO Auto-generated method stub
		return userDAO.query();
	}
}
