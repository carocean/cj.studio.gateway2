package cj.test.website.service;

import cj.studio.ecm.IServiceAfter;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import redis.clients.jedis.Jedis;

@CjService(name = "testRedis")
public class TestRedis implements IServiceAfter {
	@CjServiceRef(refByName = "plugin.redis.@auto")
	Jedis jedis;

	@Override
	public void onAfter(IServiceSite arg0) {
		// TODO Auto-generated method stub
		System.out.println("-----"+jedis);
	}
	
}
