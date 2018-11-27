package cj.studio.orm.mongodb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

import cj.lns.chip.sos.cube.framework.ICube;
import cj.lns.chip.sos.disk.INetDisk;
import cj.lns.chip.sos.disk.NetDisk;
import cj.studio.ecm.CJSystem;
import cj.studio.ecm.IAssemblyContext;
import cj.studio.ecm.IChipPlugin;
import cj.studio.ecm.context.IElement;
import cj.studio.ecm.context.IProperty;
import cj.ultimate.gson2.com.google.gson.Gson;
import cj.ultimate.gson2.com.google.gson.reflect.TypeToken;
import cj.ultimate.util.StringUtil;

public class MongoDBPlugin implements IChipPlugin {
	Map<String, INetDisk> disks;
	private MongoClient client;

	public MongoDBPlugin() {
		disks = new HashMap<>();
	}

	@Override
	public Object getService(String serviceId) {
		int pos = serviceId.indexOf(".");
		String diskName = "";
		String cubeName = "";
		if (pos < 0) {
			diskName = serviceId;
			INetDisk disk = null;
			if (disks.containsKey(diskName)) {
				disk = disks.get(diskName);
			} else {
				disk = NetDisk.trustOpen(client, diskName);
				disks.put(diskName, disk);
			}
			return disk;
		}
		diskName = serviceId.substring(0, pos);
		cubeName = serviceId.substring(pos + 1, serviceId.length());
		INetDisk disk = null;
		if (disks.containsKey(diskName)) {
			disk = disks.get(diskName);
		} else {
			disk = NetDisk.trustOpen(client, diskName);
			disks.put(diskName, disk);
		}
		ICube cube = StringUtil.isEmpty(cubeName) ? disk.home() : disk.cube(cubeName);
		return cube;
	}

	@Override
	public void load(IAssemblyContext ctx, IElement args) {
		List<ServerAddress> seeds = new ArrayList<>();
		IProperty prop = (IProperty) args.getNode("remotes");
		if (prop != null) {
			String json = prop.getValue() == null ? "[]" : prop.getValue().getName();
			List<String> list = new Gson().fromJson(json, new TypeToken<ArrayList<String>>() {
			}.getType());
			for (String address : list) {
				int pos = address.indexOf(":");
				if (pos < 0) {
					CJSystem.logging().warn(getClass(), "mongodb的地址格式无效，将不被使用，格式是：host:port。错误配置为：" + address);
					continue;
				}
				String host = address.substring(0, pos);
				String port = address.substring(pos + 1, address.length());
				ServerAddress sa = new ServerAddress(host, Integer.valueOf(port));
				seeds.add(sa);
			}
		}

		this.client = new MongoClient(seeds);
	}

	@Override
	public void unload() {
		for (INetDisk disk : this.disks.values()) {
			disk.close();
		}
		this.disks.clear();
		client.close();
	}

}
