/*
 * Created on 8 févr. 2005
 *
 * TODO 
 */
package jrds.probe.munins;

import java.util.Iterator;
import java.util.Map;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.probe.IndexedProbe;


/**
 * @author bacchell
 *
 * TODO 
 */
public abstract class MuninsIndexedValuesProbe extends MuninsProbe  implements IndexedProbe {
	String indexKey;

	/**
	 * @param monitoredHost
	 */
	public MuninsIndexedValuesProbe(RdsHost monitoredHost, ProbeDesc pd, String indexKey) {
		super(monitoredHost, pd);
		this.indexKey = indexKey;
	}
	
	public Map filterValues(Map valuesList)
	{
		for(Iterator i = valuesList.keySet().iterator() ; i.hasNext(); ) {
			String indexedName = (String) i.next();
			String[] elements = indexedName.split("_");
			String index = elements[0];
			String rootName = elements[1];
			if( ! indexKey.equals(index))
				i.remove();
			else if(  ! nameMap.containsKey(rootName) )
				i.remove();
		}
		return valuesList;
	}
	
	/* (non-Javadoc)
	 * @see com.aol.jrds.probe.IndexedProbe#getIndexName()
	 */
	public String getIndexName() {
		return indexKey;
	}

}
