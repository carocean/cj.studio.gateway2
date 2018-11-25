package cj.studio.orm.mybatis;

import java.io.InputStream;

import org.apache.ibatis.session.SqlSessionFactory;

public interface IDBEnvironment {

	void init(InputStream mybatisConfigFile);
	void init();
	SqlSessionFactory factory();

}
