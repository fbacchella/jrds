package jrds.probe;

import java.util.HashMap;
import java.util.Map;

import jrds.Probe;
import jrds.ProbeDesc;
import jrds.RdsHost;

/**
 * All probe derived from this one will not store nor poll data
 * Graph must be created on the fly.
 * @author bacchell
 *
 */
public abstract class VirtualProbe extends Probe {
	/**
	 * The constructor that should be called by derived class
	 * @param monitoredHost
	 * @param pd
	 */
	public VirtualProbe(RdsHost monitoredHost, ProbeDesc pd) {
		super(monitoredHost, pd);
	}
	
	public Map getNewSampleValues() {
		return new HashMap();
	}

	public boolean checkStore() {
		return true;
	}

	/**
	 * This method does nothing for a virtual probe
	 * @see jrds.Probe#collect()
	 */
	public void collect() {
	}


}
