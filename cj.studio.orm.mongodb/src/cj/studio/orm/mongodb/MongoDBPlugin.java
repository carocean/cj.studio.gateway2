package cj.studio.orm.mongodb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

import cj.lns.chip.sos.cube.framework.CubeConfig;
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
                disk = NetDisk.trustOpen(client, diskName, this.getClass().getClassLoader());
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
            disk = NetDisk.trustOpen(client, diskName, this.getClass().getClassLoader());
            disks.put(diskName, disk);
        }
        ICube cube = null;
        if (StringUtil.isEmpty(cubeName)) {
            cube = disk.home();
        } else {
            if (cubeName.endsWith(":autocreate")) {
                cubeName = cubeName.substring(0, cubeName.length() - ":autocreate".length());
                if (disk.existsCube(cubeName)) {
                    cube = disk.cube(cubeName);
                } else {
                    CubeConfig conf = new CubeConfig();
                    conf.alias(cubeName);
                    cube = disk.createCube(cubeName, conf);
                }
            } else {
                cube = disk.cube(cubeName);
            }
        }
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
        IProperty enableprop = (IProperty) args.getNode("isTrustConnect");
        String isTrustConnect = "true";
        if (enableprop != null) {
            isTrustConnect = enableprop.getValue().getName();
            if (StringUtil.isEmpty(isTrustConnect)) {
                isTrustConnect = "true";
            }
        }
        if ("true".equals(isTrustConnect)) {
			this.client = new MongoClient(seeds);
			CJSystem.logging().info(getClass(), "mongodb已采用确信的模式成功建立连接");
			return;
        }

        IProperty dbprop = (IProperty) args.getNode("database");
        String database = "";
        if (dbprop == null) {
            database = "admin";
        } else {
            database = dbprop.getValue().getName();
            if (StringUtil.isEmpty(database)) {
                database = "admin";
            }
        }
        IProperty userprop = (IProperty) args.getNode("user");
        String user = "";
        if (userprop != null) {
            user = userprop.getValue().getName();
        }
        if (StringUtil.isEmpty(user)) {
            throw new RuntimeException(String.format("缺少用户名"));
        }
        IProperty passwordprop = (IProperty) args.getNode("password");
        String password = "";
        if (userprop != null) {
            password = passwordprop.getValue().getName();
        }
        if (StringUtil.isEmpty(password)) {
            throw new RuntimeException(String.format("缺少密码"));
        }

        List<MongoCredential> credential = new ArrayList<>();
        MongoCredential m = MongoCredential.createCredential(
                user, database, password.toCharArray());
        credential.add(m);
        MongoClientOptions options = MongoClientOptions.builder().build();
        this.client = new MongoClient(seeds, credential, options);
		CJSystem.logging().info(getClass(), String.format("mongodb成功建立连接。database=%s user=%s password=%s",database,user,password));
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
