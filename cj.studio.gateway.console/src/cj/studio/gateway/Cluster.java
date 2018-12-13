package cj.studio.gateway;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.logging.ILogging;
import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.socket.Destination;
import cj.ultimate.gson2.com.google.gson.Gson;
import cj.ultimate.gson2.com.google.gson.reflect.TypeToken;

public class Cluster implements ICluster {
	static ILogging log = CJSystem.logging();
	Map<String, Destination> valids;
	Map<String, Destination> invalids;
	
	public Cluster() {
		this.valids = new HashMap<>();
		this.invalids = new HashMap<>();
	}

	@Override
	public Collection<Destination> getDestinations() {
		return valids.values();
	}

	@Override
	public boolean containsValid(String domain) {
		return valids.containsKey(domain);
	}

	@Override
	public void addDestination(Destination dest) {
		if (valids.containsKey(dest.getName())) {
			throw new EcmException(String.format("已存在目标：%s", dest.getName()));
		}
		valids.put(dest.getName(), dest);
	}

	@Override
	public void removeDestination(String domain) {
		valids.remove(domain);

	}

	@Override
	public String toJson() {
		Map<String, Collection<Destination>> map = new HashMap<>();
		map.put("valids", this.valids.values());
		map.put("invalids", this.invalids.values());
		return new Gson().toJson(map);
	}

	public static Cluster fromJson(String json) {
		Cluster c = new Cluster();
		HashMap<String, ArrayList<Destination>> map = new Gson().fromJson(json,
				new TypeToken<HashMap<String, ArrayList<Destination>>>() {
				}.getType());
		ArrayList<Destination> dests = map.get("valids");
		for (Destination d : dests) {
			if (c.valids.containsKey(d.getName())) {
				log.warn(String.format("目标：%s已存在，已忽略", d.getName()));
				continue;
			}
			c.valids.put(d.getName(), d);
		}
		ArrayList<Destination> dests2 = map.get("invalids");
		for (Destination d : dests2) {
			if (c.invalids.containsKey(d.getName())) {
				log.warn(String.format("目标：%s已存在，已忽略", d.getName()));
				continue;
			}
			c.invalids.put(d.getName(), d);
		}
		return c;
	}

	@Override
	public Destination getDestination(String domain) {
		return valids.get(domain);
	}

	@Override
	public Collection<Destination> listInvalids() {
		return invalids.values();
	}

	@Override
	public void invalid(String domain, String invalid_uri, String cause) {
		Destination dest = valids.get(domain);
		Destination idest = null;
		if (invalids.containsKey(domain)) {
			idest = invalids.get(domain);
		} else {
			idest = new Destination();
			invalids.put(domain, idest);
			idest.getProps().putAll(dest.getProps());
			idest.getProps().put("cause", cause);
			idest.setName(domain);
		}

		List<String> list = dest.getUris();
		List<String> ilist = idest.getUris();
		String[] uris = list.toArray(new String[0]);
		for (int i = 0; i < uris.length; i++) {
			String uri = uris[i];
			if (invalid_uri.equals(uri)) {
				ilist.add(uri);
				list.remove(i);
			}
		}
		if (list.isEmpty()) {// 整个无效
			valids.remove(domain);
		}
	}

	@Override
	public void invalidDestination(String domain, String cause) {
		Destination dest = valids.get(domain);
		String[] list = dest.getUris().toArray(new String[0]);
		for (String uri : list) {
			invalid(domain, uri, cause);
		}
	}

	@Override
	// 添加到无效目标列表，并标明原因，如果余下的有有效url，则将无效的url移除，如果没有余下有效的，则移除整个目标。
	public void invalid(String domain, String invalid_uri, CircuitException cause) {
		invalid(domain, invalid_uri, cause.getMessage());
	}

	@Override
	// 使指定的url有效.由运维人员调用
	public void valid(String domain, String invalid_uri) {
		Destination dest = valids.get(domain);
		Destination idest = invalids.get(domain);
		if (dest == null) {
			dest = new Destination();
			valids.put(domain, dest);
			dest.getProps().putAll(idest.getProps());
			dest.setName(domain);
			dest.getProps().remove("cause");
		}
		List<String> list = dest.getUris();
		if (!list.contains(invalid_uri)) {
			list.add(invalid_uri);
		}
		String[] arr = idest.getUris().toArray(new String[0]);
		for (int i = 0; i < arr.length; i++) {
			idest.getUris().remove(i);
		}
		if (idest.getUris().isEmpty()) {
			invalids.remove(domain);
		}
	}

	@Override
	public void validDestination(String domain) {
		Destination idest = invalids.get(domain);
		String[] ilist = idest.getUris().toArray(new String[0]);
		for (String iurl : ilist) {
			valid(domain, iurl);
		}
	}

	@Override
	public boolean containsInvalid(String domain) {
		return invalids.containsKey(domain);
	}


}
