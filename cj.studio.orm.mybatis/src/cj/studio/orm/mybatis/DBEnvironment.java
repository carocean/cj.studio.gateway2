package cj.studio.orm.mybatis;

import java.io.InputStream;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.annotation.CjService;

@CjService(name = "DBEnvironment")
public class DBEnvironment implements IDBEnvironment {

	private SqlSessionFactory factory;

	@Override
	public void init() {
		InputStream in=this.getClass().getClassLoader().getResourceAsStream("cj/db/mybatis.xml");
		if(in==null) {
			throw new EcmException("mybatis.xml不存在:cj/db/mybatis.xml");
		}
		init(in);
	}

	@Override
	public void init(InputStream mybatisConfigFile) {
		this.factory = new SqlSessionFactoryBuilder().build(mybatisConfigFile);
	}
	@Override
	public SqlSessionFactory factory() {
		return factory;
	}
}
