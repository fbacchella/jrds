package jrds;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jrds.factories.ArgFactory;
import jrds.factories.ProbeFactory;
import jrds.factories.xml.JrdsNode;

public class Macro {
	private class ProbeInfo {
		Map<String, String> attrs;
		JrdsNode args;
	}
	private final Set<ProbeInfo> probeList = new HashSet<ProbeInfo>();
	private final Set<String> tags = new HashSet<String>();
	private ProbeFactory pf;
	private String name;

	public Macro(ProbeFactory pf) {
		this.pf = pf;
	}

	public  Set<Probe<?,?>> populate(RdsHost host, Map<String, String> properties) {
		Set<Probe<?,?>> probes = new HashSet<Probe<?,?>>();
		for(ProbeInfo pi: probeList) {
			Map<String, String> attrs = pi.attrs;
			String className = attrs.get("type");
			List<?> constArgs = ArgFactory.makeArgs(pi.args, properties, host);
			Probe<?,?> newRdsRrd = pf.makeProbe(className);
			if(newRdsRrd == null)
				continue;

			String label = attrs.get("label");
			if(label != null && ! "".equals(label)) {
				label = jrds.Util.parseTemplate(label, properties, host);
				newRdsRrd.setLabel(label);
			}

			newRdsRrd.setHost(host);

			String connexionName = attrs.get("connection");
			if(newRdsRrd instanceof ConnectedProbe && connexionName != null && ! "".equals(connexionName)) {
				((ConnectedProbe)newRdsRrd).setConnectionName(connexionName);
			}

			if(pf.configure(newRdsRrd, constArgs))
				probes.add(newRdsRrd);

		}
		for(String tag: tags) {
			host.addTag(tag);
		}
		return probes;
	}

	public void put(Map<String, String> attrMap, JrdsNode args) {
		ProbeInfo pi = new ProbeInfo();
		pi.attrs = attrMap;
		pi.args = args;
		probeList.add(pi);
	}

	public void addTag(String tag) {
		tags.add(tag);
	}

	//	@Override
	//	public String toString() {
	//		StringBuilder ret =new StringBuilder();
	//		ret.append("[");
	//		for(Object[] probes: probeList) {
	//			ret.append(probes[0]);
	//			ret.append(probes[1]);
	//			ret.append(",");
	//		}
	//		ret.setCharAt(ret.length()-1, ']');
	//		return "Macro"+ ret  ;
	//	}

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
