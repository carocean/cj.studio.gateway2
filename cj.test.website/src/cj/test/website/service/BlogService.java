package cj.test.website.service;

import java.util.List;

import cj.studio.ecm.annotation.CjBridge;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.orm.mybatis.annotation.CjTransaction;
import cj.test.website.bo.BlogBO;
import cj.test.website.dao.IBlogDAO;
@CjBridge(aspects="@transaction")
@CjService(name="blogService")
public class BlogService implements IBlogService {
	
	@CjServiceRef(refByName="plugin.mybatis.cj.test.plugin.IStoryDAO")
	Object ArticleDAO;
	@CjServiceRef(refByName="mybatis.cj.test.website.dao.IBlogDAO")
	IBlogDAO blogDAO;
	/* (non-Javadoc)
	 * @see cj.test.website.service.IBlogService#saveBlog(cj.test.website.bo.BlogBO)
	 */
	@CjTransaction
	@Override
	public void saveBlog(BlogBO bo) {
		blogDAO.save(bo);
	}
	
}
