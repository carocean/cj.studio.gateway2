package cj.studio.orm.redis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IAssemblyContext;
import cj.studio.ecm.IChipPlugin;
import cj.studio.ecm.context.IElement;
import cj.studio.ecm.context.INode;
import cj.studio.ecm.context.IProperty;
import cj.ultimate.gson2.com.google.gson.Gson;
import cj.ultimate.gson2.com.google.gson.reflect.TypeToken;
import cj.ultimate.util.StringUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisDBPlugin implements IChipPlugin {

	private Map<String, JedisPool> pools;
	List<Jedis> container;

	public RedisDBPlugin() {
		container = new ArrayList<>();
		pools = new HashMap<>();
	}

	@Override
	public Object getService(String name) {
		JedisPool pool = null;
		if ("@auto".equals(name)) {
			JedisPool[] arr = pools.values().toArray(new JedisPool[0]);
			pool = arr[Math.abs(UUID.randomUUID().hashCode()) % arr.length];
		} else {
			pool = pools.get(name);
		}
		if (pool == null)
			return null;
		Jedis jedis = pool.getResource();
		if (container.contains(jedis)) {
			return jedis;
		}
		container.add(jedis);
		return jedis;
	}

	@Override
	public void load(IAssemblyContext ctx, IElement e) {
		String names[] = e.enumNodeNames();
		for (String name : names) {
			IProperty prop = (IProperty) e.getNode(name);
			loadRedisDBList(prop);
		}

	}

	private void loadRedisDBList(IProperty e) {
		JedisPoolConfig conf = new JedisPoolConfig();
		String name = e.getName();
		INode n = e.getValue();
		if (n == null) {
			throw new EcmException(String.format("redis连接名：%s 缺少属性定义", name));
		}
		String v = n.getName();
		if (v == null || v.equals("")) {
			throw new EcmException(String.format("redis连接名：%s 缺少属性定义", name));
		}
		Map<String, String> props = new Gson().fromJson(v, new TypeToken<HashMap<String, String>>() {
		}.getType());

		String hostStr = props.get("host");
		if (StringUtil.isEmpty(hostStr)) {
			throw new EcmException(String.format("redis连接名：%s的属性host为空", name));
		}

		String maxWaitMillis = props.get("maxWaitMillis");
		if (!StringUtil.isEmpty(maxWaitMillis)) {
			conf.setMaxWaitMillis(Long.valueOf(maxWaitMillis));
		}
		String jmxEnabled = props.get("jmxEnabled");
		if (!StringUtil.isEmpty(jmxEnabled)) {
			conf.setJmxEnabled(Boolean.valueOf(jmxEnabled));
		}
		String lifo = props.get("lifo");
		if (!StringUtil.isEmpty(lifo)) {
			conf.setLifo(Boolean.valueOf(lifo));
		}
		String blockWhenExhausted = props.get("blockWhenExhausted");// 连接耗尽时是否阻塞, false报异常,ture阻塞直到超时,
		if (!StringUtil.isEmpty(blockWhenExhausted)) {
			conf.setBlockWhenExhausted(Boolean.valueOf(blockWhenExhausted));
		}
		String maxTotal = props.get("maxTotal");// 最大连接数, 默认8个
		if (!StringUtil.isEmpty(maxTotal)) {
			conf.setMaxTotal(Integer.valueOf(maxTotal));
		}
		String minIdle = props.get("minIdle");// 最大空闲连接数, 默认8个
		if (!StringUtil.isEmpty(minIdle)) {
			conf.setMinIdle(Integer.valueOf(minIdle));
		}
		String maxIdle = props.get("maxIdle");// 最小空闲连接数, 默认0
		if (!StringUtil.isEmpty(maxIdle)) {
			conf.setMaxIdle(Integer.valueOf(maxIdle));
		}
		String softMinEvictableIdleTimeMillis = props.get("softMinEvictableIdleTimeMillis");// 获取连接时的最大等待毫秒数(如果设置为阻塞时BlockWhenExhausted),如果超时就抛异常,
//		// 小于零:阻塞不确定的时间,
//		// 默认-1
		if (!StringUtil.isEmpty(softMinEvictableIdleTimeMillis)) {
			conf.setSoftMinEvictableIdleTimeMillis(Long.valueOf(softMinEvictableIdleTimeMillis));
		}
		JedisPool pool = new JedisPool(conf, hostStr);
		pools.put(name, pool);
	}

	@Override
	public void unload() {
		for (Jedis jedis : container) {
			jedis.close();
		}
		for (JedisPool p : pools.values()) {
			p.close();
		}
		pools.clear();
	}

}
