package jrds.probe.munin;

import jrds.objects.probe.IndexedProbe;

public class MuninIndexed extends Munin implements IndexedProbe {
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
	
	public String getIndexName() {
		return indexKey;
	}
}
