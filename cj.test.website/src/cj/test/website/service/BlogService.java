package cj.test.website.service;

import cj.studio.ecm.annotation.CjBridge;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.bridge.UseBridgeMode;
import cj.test.website.bo.BlogBO;
import cj.test.website.dao.IBlogDAO;
@CjBridge(aspects="transaction")
@CjService(name="blogService")
public class BlogService implements IBlogService {
	
	@CjServiceRef(refByName="blogDAO",useBridge=UseBridgeMode.auto)
	IBlogDAO blogDAO;
	/* (non-Javadoc)
	 * @see cj.test.website.service.IBlogService#saveBlog(cj.test.website.bo.BlogBO)
	 */
	@Override
	public void saveBlog(BlogBO bo) {
		blogDAO.saveDAO(bo);
	}
}
