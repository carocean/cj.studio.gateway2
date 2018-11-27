package cj.studio.orm.mybatis;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;

class SafeSqlSessionFactory implements ISafeSqlSessionFactory {
	SqlSessionFactory factory;
	ThreadLocal<SqlSession> sessions;

	public SafeSqlSessionFactory(SqlSessionFactory factory) {
		this.factory = factory;
		sessions=new ThreadLocal<>();
	}
	
	/* (non-Javadoc)
	 * @see cj.studio.orm.mybatis.ISafeSqlSessionFactory#getSession(org.apache.ibatis.session.TransactionIsolationLevel)
	 */
	@Override
	public SqlSession getSession(TransactionIsolationLevel level) {
		SqlSession session = sessions.get();
		if (session == null) {
			if (level == null) {
				session = factory.openSession(TransactionIsolationLevel.READ_COMMITTED);
			} else {
				session = factory.openSession(level);
			}
			sessions.set(session);
		}
		return session;
	}
	/**
	 * 该方法一般用于直接返回会话，尽量不用在必须创建会话之后
	 */
	/* (non-Javadoc)
	 * @see cj.studio.orm.mybatis.ISafeSqlSessionFactory#getSession()
	 */
	@Override
	public SqlSession getSession() {
		return getSession(null);
	}

	/* (non-Javadoc)
	 * @see cj.studio.orm.mybatis.ISafeSqlSessionFactory#closeSession(org.apache.ibatis.session.SqlSession)
	 */
	@Override
	public void closeSession(SqlSession session) {
		SqlSession exists = sessions.get();
		if (exists == session) {
			sessions.remove();
		}
		session.close();
	}
}
