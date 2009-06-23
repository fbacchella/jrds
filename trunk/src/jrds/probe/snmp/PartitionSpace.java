/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/
 
package jrds.probe.snmp;

import java.util.HashMap;
import java.util.Map;

import org.snmp4j.smi.OID;


/**
 * @author bacchell
 *
 * TODO 
 */
public class PartitionSpace extends RdsIndexedSnmpRrd {
	static final private OID allocUnitOid = new OID(".1.3.6.1.2.1.25.2.3.1.4");
	static final private OID totalOid = new OID(".1.3.6.1.2.1.25.2.3.1.5");
	static final private OID usedOid = new OID(".1.3.6.1.2.1.25.2.3.1.6");
	
	/**
	 * @param monitoredHost
	 * @param indexKey
	 */
	public PartitionSpace(String indexKey) {
		super(indexKey);
	}

	public PartitionSpace(String keyName, OID indexKey) {
		super(keyName, indexKey);
	}

	/**
	 * The want to store the value in octet, not in bloc size
	 * The translation is done by the probe, not the graph
	 * @see jrds.Probe#filterValues(java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	public Map<?, Number> filterValues(Map snmpVars) {
		int allocUnit = 0;
		long total = 0;
		long used = 0;
		for(Map.Entry<OID, Number> e: ((Map<OID, Number>)snmpVars).entrySet()) {
			OID oid = e.getKey();
			Number value = e.getValue();
			oid.removeLast();
			if(allocUnitOid.equals(oid)) {
				allocUnit = value.intValue();
			}
			else if(totalOid.equals(oid)) {
				total = value.longValue();
			}
			else if(usedOid.equals(oid)) {
				used = value.longValue();
			}
		}
		total *= allocUnit;
		used *= allocUnit;
		Map<OID, Number> retValue = new HashMap<OID, Number>(2);
		retValue.put(totalOid, new Long(total));
		retValue.put(usedOid, new Long(used));
		
		return retValue;
	}
}
