package jrds;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
			String className = (String) l[0];
			List constArgs = (List) l[1];
			Probe newRdsRrd = pf.makeProbe(className, host, constArgs);
			if(newRdsRrd != null) {
				HostsList.getRootGroup().addProbe(newRdsRrd);
				host.addProbe(newRdsRrd);
			}
		}
		for(String tag: tags) {
			host.addTag(tag);
		}
	}

	public void put(String className, List constArgs) {
		Object[] l = new Object[] {className, constArgs};
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
