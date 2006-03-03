package jrds;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

public class Macro {
    static private final Logger logger = Logger.getLogger(Macro.class);
    Set probeList = new HashSet();

	public void populate(RdsHost host) {
		for(Iterator i = probeList.iterator() ; i.hasNext() ;) {
			Object[] l = (Object[]) i.next();
			String className = (String) l[0];
			List constArgs = (List) l[1];
			constArgs.set(0, host);
			Probe newRdsRrd = ProbeFactory.makeProbe(className, constArgs);
			if(newRdsRrd != null) {
				host.addProbe(newRdsRrd);
				logger.debug("adding probe " + newRdsRrd );
			}
		}
	}

	public void put(Object key, Object value) {
		Object[] l = new Object[] {key, value};
		probeList.add(l);
	}
	
}
