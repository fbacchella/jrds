/*
 * Created on 8 févr. 2005
 *
 * TODO 
 */
package jrds.probe.munins;

import jrds.probe.IndexedProbe;


/**
 * @author bacchell
 *
 * TODO 
 */
public abstract class MuninIndexed extends Munin implements IndexedProbe {
	String indexKey;

	/**
	 * @param monitoredHost
	 * @param pd
	 * @param indexKey
	 */
	public boolean configure(String indexKey) {
		this.indexKey = indexKey;
		return super.configure();
	}
	
	/* (non-Javadoc)
	 * @see com.aol.jrds.probe.IndexedProbe#getIndexName()
	 */
	public String getIndexName() {
		return indexKey;
	}
}
