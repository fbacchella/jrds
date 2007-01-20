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
	private String label;

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
			
	public Set<OID> makeIndexed(Collection<OID> oids, Collection<int[]> indexes)
	{
		Set<OID> oidToGet = new HashSet<OID>(oids.size() * indexes.size());
		for(OID oidCurs: oids) {
			for(int[] j: indexes) {
				OID oidBuf = new OID(oidCurs); //(OID) oidCurs.clone();
				for(int i = 0; i <j.length; i++) {
					oidBuf.append(j[i]);
				}
				oidToGet.add(oidBuf);
			}
		}
		return oidToGet;
	}
	
	public Collection<OID> getIndexSet() {
		Collection<OID> retValue = null;
		OID indexOid = getPd().getIndexOid();
		if(indexOid != null)
			retValue = Collections.singleton(indexOid);
		return retValue;
	}
	
	public int getIndexPrefixLength() {
		return getPd().getIndexOid().size();
	}
	
	public Collection<int[]> setIndexValue() 
	{
		
		Collection<int[]> indexSubOid = null;
		if(isUniq())
			indexSubOid = new ArrayList<int[]>(1);
		else
			indexSubOid = new HashSet<int[]>();
		
		Collection<OID> soidSet = getIndexSet();
		if(soidSet == null || soidSet.size() == 0) {
			OID suffixOid = new OID(indexKey);
			indexSubOid = Collections.singleton(suffixOid.getValue());
		}
		else {
			try {
				Map<OID, Object> somevars = indexFinder.doSnmpGet(getSnmpStarter(), soidSet);

				boolean found = false;

				int newSuffixLength = 0;
				for(OID tryoid: somevars.keySet()) {
					String name = null;
					if(tryoid != null)
						name = somevars.get(tryoid).toString();
					if(name != null && matchIndex(somevars.get(tryoid))) {
						newSuffixLength = tryoid.size() - getIndexPrefixLength();
						int[] index = new int[ newSuffixLength ];
						for(int i = 0; i < index.length ; i++) {
							index[i] = tryoid.get(i + getIndexPrefixLength());
						}
						indexSubOid.add(index);
						found = true;
					}
					if(isUniq() && found)
						break;
				}
				if(found) {
					setSuffixLength(newSuffixLength);
				}
				else  {
					logger.error("index for " + indexKey + " not found for " + this);
					indexSubOid = null;
				}
			} catch (IOException e) {
				logger.error("index for " + indexKey + " not found for " + this + " because of: " + e);
			}
		}
		return indexSubOid;
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
		Collection<int[]> indexAsString = setIndexValue();
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

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
}
