package jrds.probe;

import java.util.HashMap;
import java.util.Map;

public class KstatProbeIndexed extends KstatProbe implements IndexedProbe {
	String index;
	
	private Boolean setup(int instance, Map<String, String> vars) {
		String module = jrds.Util.parseTemplate(getPd().getSpecific("module"), vars);
		String name = jrds.Util.parseTemplate(getPd().getSpecific("name"), vars);
		index = jrds.Util.parseTemplate(getPd().getSpecific("index"), vars);;
		return setup(module, instance, name);
	}
	
	public Boolean configure(String name) {
		Map<String, String> vars = new HashMap<String, String>(1);
		vars.put("name", name);
		return setup(0, vars);
	}

	public Boolean configure(String device, Integer instance) {
		Map<String, String> vars = new HashMap<String, String>(2);
		vars.put("device", device);
		vars.put("instance", instance.toString());
		return setup(instance, vars);
	}

	public Boolean configure(Integer instance) {
		Map<String, String> vars = new HashMap<String, String>(1);
		vars.put("instance", instance.toString());
		return setup(instance, vars);
	}
	
	public String getIndexName() {
		return index;
	}

}
