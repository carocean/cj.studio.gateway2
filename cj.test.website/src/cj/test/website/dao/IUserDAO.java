package cj.test.website.dao;

import java.util.List;

import cj.test.website.bo.UserBO;

public interface IUserDAO {
	void save(UserBO bo);
	void delete(String id);
	List<UserBO> query();
}
