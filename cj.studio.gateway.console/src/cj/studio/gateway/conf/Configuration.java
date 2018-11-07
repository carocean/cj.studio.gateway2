package cj.studio.gateway.conf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cj.studio.gateway.Cluster;
import cj.studio.gateway.ICluster;
import cj.studio.gateway.IConfiguration;
import cj.ultimate.gson2.com.google.gson.Gson;
import cj.ultimate.gson2.com.google.gson.reflect.TypeToken;
import cj.ultimate.util.FileHelper;
import cj.ultimate.util.StringUtil;

public class Configuration implements IConfiguration {
	String homeDir;
	Map<String, ServerInfo> servers;
	ICluster cluster;
	public Configuration(String homeDir) {
		this.homeDir=homeDir;
	}
	@Override
	public void load() {
		loadServers();
		loadCluster();
	}
	@Override
	public ICluster getCluster() {
		return cluster;
	}
	protected void loadCluster() {
		String fn = String.format("%s%sconf%scluster.json", homeDir, File.separator, File.separator);
		File f = new File(fn);
		if (!f.getParentFile().exists()) {
			f.getParentFile().mkdirs();
		}
		try {
			if(!f.exists()){
				f.createNewFile();
			}
			byte[] b = FileHelper.readFully(f);
			String json = new String(b);
			if (!StringUtil.isEmpty(json)) {
				cluster=Cluster.fromJson(json);
			}else{
				cluster=new Cluster();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
		}
		
	}
	@Override
	public void flushServers() {
		String fn = String.format("%s%sconf%sservers.json", homeDir, File.separator, File.separator);
		File f = new File(fn);
		if (!f.getParentFile().exists()) {
			f.getParentFile().mkdirs();
		}
		FileOutputStream fs = null;
		try {
			fs = new FileOutputStream(fn);
			String json = new Gson().toJson(this.servers.values());
			fs.write(json.getBytes());
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (fs != null) {
				try {
					fs.close();
				} catch (IOException e) {
				}
			}
		}
		
	}

	@Override
	public String home() {
		return homeDir;
	}
	protected void loadServers() {
		String fn = String.format("%s%sconf%sservers.json", homeDir, File.separator, File.separator);
		File f = new File(fn);
		if (!f.getParentFile().exists()) {
			f.getParentFile().mkdirs();
		}
		
		try {
			if(!f.exists()){
				f.createNewFile();
			}
			byte[] b = FileHelper.readFully(f);
			String json = new String(b);
			servers=new HashMap<>();
			if (!StringUtil.isEmpty(json)) {
				Gson g = new Gson();
				ArrayList<ServerInfo> list = g.fromJson(json, new TypeToken<ArrayList<ServerInfo>>() {
				}.getType());
				for(ServerInfo item:list){
					if(servers.containsKey(item.name)){
						System.out.println(String.format("已存在服务：%s，已忽略。", item.name));
						continue;
					}
					servers.put(item.name, item);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
		}
		
	}
	@Override
	public Set<String> enumServerNames() {
		return this.servers.keySet();
	}
	@Override
	public ServerInfo serverInfo(String name) {
		return servers.get(name);
	}
	@Override
	public boolean containsServiceName(String name) {
		return servers.containsKey(name);
	}
	@Override
	public void addServerInfo(ServerInfo item) {
		servers.put(item.name, item);
	}
	@Override
	public void removeServerInfo(String name) {
		servers.remove(name);
	}
	@Override
	public void flushCluster() {
		String fn = String.format("%s%sconf%scluster.json", homeDir, File.separator, File.separator);
		File f = new File(fn);
		if (!f.getParentFile().exists()) {
			f.getParentFile().mkdirs();
		}
		FileOutputStream fs = null;
		try {
			fs = new FileOutputStream(fn);
			String json = cluster.toJson();
			fs.write(json.getBytes());
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (fs != null) {
				try {
					fs.close();
				} catch (IOException e) {
				}
			}
		}
	}
}
