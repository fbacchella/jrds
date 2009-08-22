package jrds;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jrds.factories.ProbeFactory;

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
	public  Set<Probe> populate(RdsHost host) {
		Set<Probe> probes = new HashSet<Probe>();
		for(Object[] l: probeList) {
			Map<String, String> attrs = (Map<String, String>) l[0];
			String className = attrs.get("type");
			String label = attrs.get("label");
			List constArgs = (List) l[1];
			Probe newRdsRrd = pf.makeProbe(className, host, constArgs);
			if(newRdsRrd != null) {
				if(label != null) {
					newRdsRrd.setLabel(label);
				}
				host.getProbes().add(newRdsRrd);
				//HostsList.getRootGroup().addProbe(newRdsRrd);
			}
		}
		for(String tag: tags) {
			host.addTag(tag);
		}
		return probes;
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
