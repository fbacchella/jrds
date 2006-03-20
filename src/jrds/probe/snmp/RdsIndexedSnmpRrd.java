/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds.probe.snmp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import jrds.ProbeDesc;
import jrds.RdsHost;
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
	Collection indexAsString = null;
	OID indexOid;
	
	static final SnmpRequester indexFinder = SnmpRequester.TABULAR;
	static final SnmpRequester valueFinder = SnmpRequester.RAW;
	
	public RdsIndexedSnmpRrd(RdsHost monitoredHost, ProbeDesc pd, String indexKey) {
		super(monitoredHost, pd);
		indexOid = initIndexOid();
		this.indexKey = indexKey;
	}

	protected SnmpRequester getSnmpRequester() {
		return valueFinder;
	}
	

	protected OID initIndexOid() {
		return this.getPd().getIndexOid();
	}
	
	public String getIndexName()
	{
		return indexKey;
	}
			
	public Set makeIndexed(Collection oids, Collection indexes)
	{
		Set oidToGet = new HashSet(oids.size() * indexes.size());
		for(Iterator i = oids.iterator() ; i.hasNext() ; )  {
			OID oidCurs = (OID) i.next();
			for(Iterator j = indexes.iterator(); j.hasNext() ;) {
				OID oidBuf = (OID) oidCurs.clone();
				oidBuf.append((String)j.next());
				oidToGet.add(oidBuf);
			}
		}
		return oidToGet;
	}
	
	public Collection setIndexValue() 
	{
		
		Collection indexAsString = null;
		if(isUniq())
			indexAsString = new ArrayList(1);
		else
			indexAsString = new HashSet();
		
		Collection soidSet= new ArrayList(1);
		soidSet.add(indexOid);
		Map somevars = indexFinder.doSnmpGet(this, soidSet);
		boolean found = false;
		
		for(Iterator i = somevars.keySet().iterator(); i.hasNext() &&  ! (isUniq() && found) ;) {
			String name = null;
			OID tryoid = (OID)i.next();
			if(tryoid != null)
				name = somevars.get(tryoid).toString();
			if(name != null && matchIndex(indexKey, name)) {
				int index = tryoid.removeLast();
				indexAsString.add(Integer.toString(index));
				found = true;
			}
		}
		
		if(! found) {
			logger.error("index for " + indexKey + " not found for host " + getHost().getName());
			indexAsString = null;
		}
		return indexAsString;
	}

	/**
	 * This method check if the tried value match the index
	 * @param index the index value 
	 * @param key the found key tried
	 * @return
	 */
	public boolean matchIndex(String index, String key) {
		return index.equals(key);
	}
	/**
	 * @see jrds.probe.snmp.SnmpProbe#getOidSet()
	 */
	public Set getOidSet() {
		Set retValue = null;
		Collection indexAsString = setIndexValue();
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
