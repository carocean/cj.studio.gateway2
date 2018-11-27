package cj.studio.orm.mybatis;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.TransactionIsolationLevel;

public interface ISafeSqlSessionFactory {

	SqlSession getSession(TransactionIsolationLevel level);

	SqlSession getSession();

	void closeSession(SqlSession session);

}