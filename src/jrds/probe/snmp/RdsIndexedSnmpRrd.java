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

	static public final String INDEXOIDNAME="index";

	Object key;
	String indexKey;
	private OID indexOid;

	static final SnmpRequester indexFinder = SnmpRequester.TABULAR;
	static final SnmpRequester valueFinder = SnmpRequester.RAW;

	public void configure(String indexKey) {
		this.key = indexKey;
		this.indexKey = indexKey;
	}

	public void configure(Integer indexKey) {
		this.key = indexKey;
		this.indexKey = String.valueOf(indexKey);
	}

	/**
	 * If the key type is an OID, it is directly the OID suffix, no look up will be done
	 * @param indexKey
	 */
	public void configure(OID indexKey) {
		this.key = indexKey;
		this.indexKey = indexKey.toString();
	}

	public void configure(String keyName, OID indexKey) {
		this.key = indexKey;
		this.indexKey = keyName;
	}

	protected SnmpRequester getSnmpRequester() {
		return valueFinder;
	}

	/* (non-Javadoc)
	 * @see jrds.probe.snmp.SnmpProbe#readSpecific()
	 */
	@Override
	public boolean readSpecific() {
		getPd().addSpecific(REQUESTERNAME, "RAW");
		String oidString =  getPd().getSpecific(INDEXOIDNAME);
		if(oidString != null && oidString.length() > 0) 
			indexOid = new OID(oidString);
		return super.readSpecific();
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
				OID oidBuf = new OID(oidCurs);
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
		if(indexOid != null)
			retValue = Collections.singleton(indexOid);
		return retValue;
	}

	public int getIndexPrefixLength() {
		return indexOid.size();
	}

	/**
	 * Generate the index suffix for the probe
	 * @return
	 */
	public Collection<int[]> setIndexValue() 
	{

		Collection<int[]> indexSubOid = null;
		if(isUniq())
			indexSubOid = new ArrayList<int[]>(1);
			else
				indexSubOid = new HashSet<int[]>();

				Collection<OID> soidSet = getIndexSet();

				//If we already have the key, no need to search for it
				if(key instanceof OID) {
					OID suffixOid = (OID) key;
					indexSubOid = Collections.singleton(suffixOid.getValue());
					setSuffixLength(suffixOid.size());
				}
				//If no index OID, the indexKey is already the snmp suffix.
				else if(soidSet == null || soidSet.size() == 0) {
					OID suffixOid = new OID(indexKey);
					indexSubOid = Collections.singleton(suffixOid.getValue());
					setSuffixLength(suffixOid.size());
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
	public boolean matchIndex(Object readKey) {
		return key.equals(readKey);
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
}
