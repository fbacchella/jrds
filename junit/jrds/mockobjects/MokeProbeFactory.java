package jrds.mockobjects;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jrds.GraphDesc;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.factories.ProbeFactory;

public class MokeProbeFactory extends ProbeFactory {
	static Map<String, ProbeDesc> probeDescMap = Collections.emptyMap();
	static Map<String, GraphDesc> graphDescMap = Collections.emptyMap();
	static PropertiesManager  pm = new PropertiesManager();
	static boolean legacymode = false;

	public MokeProbeFactory() {
		super(probeDescMap, graphDescMap, pm);
	}

	/* (non-Javadoc)
	 * @see jrds.factories.ProbeFactory#makeProbe(jrds.ProbeDesc, java.util.List)
	 */
	public Probe<?,?> makeProbe(ProbeDesc pd) {
		return new MokeProbe<String, Number>(pd);
	}
	public Probe<?,?> makeProbe(String type) {
		return new MokeProbe<String, Number>(type);
	}

	/* (non-Javadoc)
	 * @see jrds.factories.ProbeFactory#configure(jrds.Probe, java.util.List)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean configure(Probe<?, ?> p, List<?> constArgs) {
		if(p instanceof MokeProbe) {
			MokeProbe<?,?> mp = (MokeProbe<?,?>)p;
			mp.configure();
			mp.setArgs(constArgs);
		}
		return true;
	}
}
