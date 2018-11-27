package cj.test.website.service;

import java.util.List;

import cj.test.website.bo.UserBO;

public interface IUserService {

	void saveUser(UserBO bo);
	List<UserBO> query();
}
