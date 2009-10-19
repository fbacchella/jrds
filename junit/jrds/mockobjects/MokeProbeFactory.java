package jrds.mockobjects;

import java.util.Collections;
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
		return new MokeProbe<String, Number>();
	}
}
