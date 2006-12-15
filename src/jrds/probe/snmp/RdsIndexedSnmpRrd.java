/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds.probe.snmp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jrds.probe.IndexedProbe;
import jrds.snmp.SnmpRequester;

import org.apache.log4j.Logger;
import org.snmp4j.smi.OID;


/**
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public class RdsIndexedSnmpRrd extends SnmpProbe implements IndexedProbe {
	
	static final private Logger logger = Logger.getLogger(RdsIndexedSnmpRrd.class);

	String indexKey;
	int indexKeyNum;
	Collection indexAsString = null;

	static final SnmpRequester indexFinder = SnmpRequester.TABULAR;
	static final SnmpRequester valueFinder = SnmpRequester.RAW;
	
	public RdsIndexedSnmpRrd(String indexKey) {
		this.indexKey = indexKey;
	}
	
	public RdsIndexedSnmpRrd(Integer indexKey) {
		this.indexKeyNum = indexKey;
		this.indexKey = String.valueOf(indexKey);
	}
	
	protected SnmpRequester getSnmpRequester() {
		return valueFinder;
	}

	public String getIndexName()
	{
		return indexKey;
	}
			
	public Set<OID> makeIndexed(Collection<OID> oids, Collection<String> indexes)
	{
		Set<OID> oidToGet = new HashSet<OID>(oids.size() * indexes.size());
		for(OID oidCurs: oids) {
			for(String j: indexes) {
				OID oidBuf = (OID) oidCurs.clone();
				oidBuf.append(j);
				oidToGet.add(oidBuf);
			}
		}
		return oidToGet;
	}
	
	public Collection<OID> getIndexSet() {
		return Collections.singleton(getPd().getIndexOid());
	}
	
	public Collection<String> setIndexValue() 
	{
		
		Collection<String> indexAsString = null;
		if(isUniq())
			indexAsString = new ArrayList<String>(1);
		else
			indexAsString = new HashSet<String>();
		
		Collection<OID> soidSet= getIndexSet();
		try {
			Map<OID, Object> somevars = indexFinder.doSnmpGet(getSnmpStarter(), soidSet);

			boolean found = false;
			
			for(OID tryoid: somevars.keySet()) {
				String name = null;
				if(tryoid != null)
					name = somevars.get(tryoid).toString();
				if(name != null && matchIndex(somevars.get(tryoid))) {
					int index = tryoid.removeLast();
					indexAsString.add(Integer.toString(index));
					found = true;
				}
				if(isUniq() && found)
					break;
			}
			
			if(! found) {
				logger.error("index for " + indexKey + " not found for " + this);
				indexAsString = null;
			}
		} catch (IOException e) {
			logger.error("index for " + indexKey + " not found for " + this + " because of: " + e);
		}
		return indexAsString;
	}

	/**
	 * This method check if the tried value match the index
	 * @param index the index value 
	 * @param key the found key tried
	 * @return
	 */
	public boolean matchIndex(Object key) {
		boolean match = false;
		if(key instanceof Integer)
			match = ((Integer)key == indexKeyNum);
		else
			match =  indexKey.equals(key.toString());
		return match;
	}
	
	/**
	 * @see jrds.probe.snmp.SnmpProbe#getOidSet()
	 */
	public Set<OID> getOidSet() {
		Set<OID> retValue = null;
		Collection<String> indexAsString = setIndexValue();
		if(indexAsString != null)
			retValue = makeIndexed(getOidNameMap().keySet(), indexAsString);
		return retValue;
	}

	/**
	 * @return Returns the uniq.
	 */
	public boolean isUniq() {
		return this.getPd().isUniqIndex();
	}
}
