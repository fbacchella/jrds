package jrds;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jrds.factories.ProbeFactory;
import jrds.probe.IndexedProbe;

public class Macro {
	private final Set<Object[]> probeList = new HashSet<Object[]>();
	private final Set<String> tags = new HashSet<String>();
	private ProbeFactory pf;
	private String name;

	public Macro(ProbeFactory pf) {
		super();
		this.pf = pf;
	}

	@SuppressWarnings("unchecked")
	public void populate(RdsHost host) {
		for(Object[] l: probeList) {
			Map<String, String> attrs = (Map) l[0];
			String className = attrs.get("type");
			String label = attrs.get("label");
			List constArgs = (List) l[1];
			Probe newRdsRrd = pf.makeProbe(className, constArgs);
			if(newRdsRrd != null) {
				if(newRdsRrd instanceof IndexedProbe && label != null) {
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
		StringBuilder ret =new StringBuilder();
		ret.append("[");
		for(Object[] probes: probeList) {
			ret.append(probes[0]);
			ret.append(probes[1]);
			ret.append(",");
		}
		ret.setCharAt(ret.length()-1, ']');
		return "Macro"+ ret  ;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
}
