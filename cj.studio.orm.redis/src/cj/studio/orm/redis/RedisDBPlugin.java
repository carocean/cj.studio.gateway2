package cj.studio.orm.redis;

import java.util.List;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IAssemblyContext;
import cj.studio.ecm.IChipPlugin;
import cj.studio.ecm.context.IElement;
import cj.studio.ecm.context.INode;
import cj.studio.ecm.context.IProperty;
import cj.ultimate.util.StringUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisDBPlugin implements IChipPlugin {

	private JedisPool pool;
	List<Jedis> container;

	@Override
	public Object getService(String name) {
		Jedis jedis = pool.getResource();
		if (container.contains(jedis)) {
			return jedis;
		}
		container.add(jedis);
		return null;
	}

	@Override
	public void load(IAssemblyContext ctx, IElement e) {
		JedisPoolConfig conf = new JedisPoolConfig();
		IProperty host = (IProperty) e.getNode("host");
		String hostStr="";
		if (host != null) {
			INode n = host.getValue();
			if (n != null&&!StringUtil.isEmpty(n.getName())) {
				hostStr=n.getName();
			}
		}
		if(StringUtil.isEmpty(hostStr)) {
			throw new EcmException("redis属性host为空");
		}
		IProperty maxWaitMillis = (IProperty) e.getNode("maxWaitMillis");
		if (maxWaitMillis != null) {
			INode n = maxWaitMillis.getValue();
			if (n != null&&!StringUtil.isEmpty(n.getName())) {
				conf.setMaxWaitMillis(Long.valueOf(n.getName()));
			}
		}
		IProperty jmxEnabled = (IProperty) e.getNode("jmxEnabled");// 是否启用pool的jmx管理功能, 默认true
		if (jmxEnabled != null) {
			INode n = jmxEnabled.getValue();
			if (n != null&&!StringUtil.isEmpty(n.getName())) {
				conf.setJmxEnabled(Boolean.valueOf(n.getName()));
			}
		}
		IProperty lifo = (IProperty) e.getNode("lifo");// 是否启用后进先出, 默认true
		if (lifo != null) {
			INode n = lifo.getValue();
			if (n != null&&!StringUtil.isEmpty(n.getName())) {
				conf.setLifo(Boolean.valueOf(n.getName()));
			}
		}
		IProperty blockWhenExhausted = (IProperty) e.getNode("blockWhenExhausted");//// 连接耗尽时是否阻塞, false报异常,ture阻塞直到超时,
		if (blockWhenExhausted != null) {
			INode n = blockWhenExhausted.getValue();
			if (n != null&&!StringUtil.isEmpty(n.getName())) {
				conf.setBlockWhenExhausted(Boolean.valueOf(n.getName()));
			}
		} //// 默认true
		IProperty maxTotal = (IProperty) e.getNode("maxTotal");// 最大连接数, 默认8个
		if (maxTotal != null) {
			INode n = maxTotal.getValue();
			if (n != null&&!StringUtil.isEmpty(n.getName())) {
				conf.setMaxTotal(Integer.valueOf(n.getName()));
			}
		}
		IProperty minIdle = (IProperty) e.getNode("minIdle");// 最大空闲连接数, 默认8个
		if (minIdle != null) {
			INode n = minIdle.getValue();
			if (n != null&&!StringUtil.isEmpty(n.getName())) {
				conf.setMinIdle(Integer.valueOf(n.getName()));
			}
		}
		IProperty maxIdle = (IProperty) e.getNode("maxIdle");// 最小空闲连接数, 默认0
		if (maxIdle != null) {
			INode n = maxIdle.getValue();
			if (n != null&&!StringUtil.isEmpty(n.getName())) {
				conf.setMaxIdle(Integer.valueOf(n.getName()));
			}
		}
		IProperty softMinEvictableIdleTimeMillis = (IProperty) e.getNode("softMinEvictableIdleTimeMillis");// 获取连接时的最大等待毫秒数(如果设置为阻塞时BlockWhenExhausted),如果超时就抛异常,
																											// 小于零:阻塞不确定的时间,
																											// 默认-1
		if (softMinEvictableIdleTimeMillis != null) {
			INode n = softMinEvictableIdleTimeMillis.getValue();
			if (n != null&&!StringUtil.isEmpty(n.getName())) {
				conf.setSoftMinEvictableIdleTimeMillis(Long.valueOf(n.getName()));
			}
		}
		pool = new JedisPool(conf, hostStr);
	}

	@Override
	public void unload() {
		for (Jedis jedis : container) {
			jedis.close();
		}
		pool.close();
	}

}
