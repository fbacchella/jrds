package jrds;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

public class Macro {
    static private final Logger logger = Logger.getLogger(Macro.class);
    Set<Object[]> probeList = new HashSet<Object[]>();

	public void populate(RdsHost host) {
		for(Object[] l: probeList) {
			String className = (String) l[0];
			List constArgs = (List) l[1];
			Probe newRdsRrd = ProbeFactory.makeProbe(className, host, constArgs);
			if(newRdsRrd != null) {
				host.addProbe(newRdsRrd);
				logger.debug("adding probe " + newRdsRrd );
			}
		}
	}

	public void put(String key, List value) {
		Object[] l = new Object[] {key, value};
		probeList.add(l);
	}
	
}
