package cj.studio.orm.redis;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.context.IServiceContainerMonitor;
import cj.ultimate.gson2.com.google.gson.Gson;
import cj.ultimate.util.StringUtil;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.util.HashSet;
import java.util.Set;

public class RedisServiceContainerMonitor implements IServiceContainerMonitor {
    JedisCluster jedisCluster;

    @Override
    public void onBeforeRefresh(IServiceSite site) {
        String addresses = site.getProperty("redis.cluster.addresses");
        if (StringUtil.isEmpty(addresses)) {
            throw new EcmException("程序集属性文件中缺少redis.cluster.addresses属性配置");
        }
        String connectionTimeout = site.getProperty("redis.cluster.connectionTimeout");
        int connectionTimeoutInt = StringUtil.isEmpty(connectionTimeout) ? 3000 : Integer.valueOf(connectionTimeout);
        String soTimeout = site.getProperty("redis.cluster.soTimeout");
        int soTimeoutInt = StringUtil.isEmpty(soTimeout) ? 2000 : Integer.valueOf(soTimeout);
        String maxAttempts = site.getProperty("redis.cluster.maxAttempts");
        int maxAttemptsInt = StringUtil.isEmpty(maxAttempts) ? 5 : Integer.valueOf(maxAttempts);
        String password = site.getProperty("redis.cluster.password");

        Set<String> nodeSet = new Gson().fromJson(addresses, HashSet.class);
        Set<HostAndPort> nodes = new HashSet<>();
        for (String n : nodeSet) {
            int pos = n.indexOf(":");
            if (pos < 0) {
                throw new EcmException("地址格式错误，写法应为：host:port。地址：" + n);
            }
            String host = n.substring(0, pos);
            String port = n.substring(pos + 1);
            nodes.add(new HostAndPort(host, Integer.parseInt(port)));
        }
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        config(site, poolConfig);
        jedisCluster = new JedisCluster(nodes, connectionTimeoutInt, soTimeoutInt, maxAttemptsInt, password, poolConfig);
        site.addService("@.redis.cluster", jedisCluster);
    }

    public static void main(String[] args) {
        Set<HostAndPort> nodes = new HashSet<>();
        nodes.add(new HostAndPort("47.105.165.186", 7001));
        nodes.add(new HostAndPort("47.105.165.186", 7002));
        nodes.add(new HostAndPort("47.105.165.186", 7003));
        nodes.add(new HostAndPort("47.105.165.186", 7004));
        nodes.add(new HostAndPort("47.105.165.186", 7005));
        nodes.add(new HostAndPort("47.105.165.186", 7006));
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        JedisCluster jedisCluster = new JedisCluster(nodes, 1000, 1000, 1000, "!jofers0408", poolConfig);
        jedisCluster.close();
    }

    private void config(IServiceSite site, GenericObjectPoolConfig conf) {
        String maxWaitMillis = site.getProperty("maxWaitMillis");
        if (!StringUtil.isEmpty(maxWaitMillis)) {
            conf.setMaxWaitMillis(Long.valueOf(maxWaitMillis));
        }
        String jmxEnabled = site.getProperty("jmxEnabled");
        if (!StringUtil.isEmpty(jmxEnabled)) {
            conf.setJmxEnabled(Boolean.valueOf(jmxEnabled));
        }
        String lifo = site.getProperty("lifo");
        if (!StringUtil.isEmpty(lifo)) {
            conf.setLifo(Boolean.valueOf(lifo));
        }
        String blockWhenExhausted = site.getProperty("blockWhenExhausted");// 连接耗尽时是否阻塞, false报异常,ture阻塞直到超时,
        if (!StringUtil.isEmpty(blockWhenExhausted)) {
            conf.setBlockWhenExhausted(Boolean.valueOf(blockWhenExhausted));
        }
        String maxTotal = site.getProperty("maxTotal");// 最大连接数, 默认8个
        if (!StringUtil.isEmpty(maxTotal)) {
            conf.setMaxTotal(Integer.valueOf(maxTotal));
        }
        String minIdle = site.getProperty("minIdle");// 最大空闲连接数, 默认8个
        if (!StringUtil.isEmpty(minIdle)) {
            conf.setMinIdle(Integer.valueOf(minIdle));
        }
        String maxIdle = site.getProperty("maxIdle");// 最小空闲连接数, 默认0
        if (!StringUtil.isEmpty(maxIdle)) {
            conf.setMaxIdle(Integer.valueOf(maxIdle));
        }
        String softMinEvictableIdleTimeMillis = site.getProperty("softMinEvictableIdleTimeMillis");// 获取连接时的最大等待毫秒数(如果设置为阻塞时BlockWhenExhausted),如果超时就抛异常,
//		// 小于零:阻塞不确定的时间,
//		// 默认-1
        if (!StringUtil.isEmpty(softMinEvictableIdleTimeMillis)) {
            conf.setSoftMinEvictableIdleTimeMillis(Long.valueOf(softMinEvictableIdleTimeMillis));
        }
    }

    @Override
    public void onAfterRefresh(IServiceSite site) {

    }
}
