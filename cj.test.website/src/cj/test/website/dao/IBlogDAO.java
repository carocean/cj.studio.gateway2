package cj.test.website.dao;

import org.apache.ibatis.annotations.Insert;

import cj.test.website.bo.BlogBO;

public interface IBlogDAO {
	@Insert("insert into blog(id,name) values(#{id},#{name})")
	void save(BlogBO bo);
	
}