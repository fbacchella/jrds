package jrds.probe;

import java.util.Map;

import jrds.Probe;

/**
 * All probe derived from this one will not store nor poll data
 * Graph must be created on the fly.
 * @author bacchell
 *
 */
public abstract class VirtualProbe extends Probe {
	public Map<?, ?> getNewSampleValues() {
		return java.util.Collections.EMPTY_MAP;
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
