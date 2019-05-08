package cj.studio.orm.mybatis;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;

import cj.studio.ecm.CJSystem;

class SafeSqlSessionFactory implements ISafeSqlSessionFactory {
	SqlSessionFactory factory;
	ThreadLocal<SqlSessionWrapper> sessions;

	public SafeSqlSessionFactory(SqlSessionFactory factory) {
		this.factory = factory;
		sessions = new ThreadLocal<>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * cj.studio.orm.mybatis.ISafeSqlSessionFactory#getSession(org.apache.ibatis.
	 * session.TransactionIsolationLevel)
	 */
	@Override
	public synchronized SqlSession getSession(TransactionIsolationLevel level) {
		SqlSessionWrapper wrapper = sessions.get();
		if (wrapper == null) {
			SqlSession session = null;
			if (level == null) {
				session = factory.openSession(TransactionIsolationLevel.READ_COMMITTED);
			} else {
				session = factory.openSession(level);
			}
			wrapper = new SqlSessionWrapper();
			wrapper.session = session;
			sessions.set(wrapper);
		}
		wrapper.refCount.incrementAndGet();
		return wrapper.session;
	}

	/**
	 * 该方法一般用于直接返回会话，尽量不用在必须创建会话之后
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see cj.studio.orm.mybatis.ISafeSqlSessionFactory#getSession()
	 */
	@Override
	public SqlSession getSession() {
		return getSession(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * cj.studio.orm.mybatis.ISafeSqlSessionFactory#closeSession(org.apache.ibatis.
	 * session.SqlSession)
	 */
	@Override
	public synchronized void closeSession(SqlSession session) {
		SqlSessionWrapper exists = sessions.get();
		if (exists == null)
			return;
		if (exists.refCount.decrementAndGet() < 1) {
			if(!exists.isCommited.get()) {
				exists.session.commit(exists.isForce.get());
			}
			sessions.remove();
			session.close();
			CJSystem.logging().debug(getClass(), "SqlSession closed.");
		}
	}

	@Override
	public synchronized void commit(boolean force) {
		SqlSessionWrapper exists = sessions.get();
		if (exists == null)
			return;
		if (exists.refCount.get() <= 1) {
			exists.session.commit(force);
			exists.isCommited.set(true);
			exists.isForce.set(force);
		}
	}

	@Override
	public synchronized void rollback() {
		SqlSessionWrapper exists = sessions.get();
		if (exists == null)
			return;

		exists.session.rollback(true);//不管嵌套多少层，只要调用了rollback就执行
		exists.refCount.set(0);
	}

	class SqlSessionWrapper {
		AtomicInteger refCount;
		AtomicBoolean isForce;
		AtomicBoolean isCommited;
		SqlSession session;
		public SqlSessionWrapper() {
			refCount=new AtomicInteger(0);
			isForce=new AtomicBoolean(false);
			isCommited=new AtomicBoolean(false);
		}
	}
}
