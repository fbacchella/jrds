package jrds;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jrds.probe.IndexedProbe;

public class Macro {
	private Set<Object[]> probeList = new HashSet<Object[]>();
	private final Set<String> tags = new HashSet<String>();
	ProbeFactory pf;

	public Macro(ProbeFactory pf) {
		super();
		this.pf = pf;
	}

	public void populate(RdsHost host) {
		for(Object[] l: probeList) {
			Map map = (Map) l[0];
			Map<String, String> attrs = map;
			String className = attrs.get("type");
			String label = attrs.get("label");
			List constArgs = (List) l[1];
			Probe newRdsRrd = pf.makeProbe(className, constArgs);
			if(newRdsRrd != null) {
				if(newRdsRrd instanceof IndexedProbe && label != null) {
					//logger.debug("Adding label " + label + " to "  + newRdsRrd);
					((IndexedProbe)newRdsRrd).setLabel(label);
				}

				host.addProbe(newRdsRrd);
				HostsList.getRootGroup().addProbe(newRdsRrd);
			}
		}
		for(String tag: tags) {
			host.addTag(tag);
		}
	}

	public void put(Object[] l) {
		probeList.add(l);
	}

	public void addTag(String tag) {
		tags.add(tag);
	}

	@Override
	public String toString() {
		StringBuffer ret =new StringBuffer();
		ret.append("[");
		for(Object[] probes: probeList) {
			ret.append(probes[0]);
			ret.append(probes[1]);
			ret.append(",");
		}
		ret.setCharAt(ret.length()-1, ']');
		return "Macro"+ ret  ;
	}
}
