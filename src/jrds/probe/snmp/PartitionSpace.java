/*
 * Created on 27 déc. 2004
 *
 * TODO 
 */
package jrds.probe.snmp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jrds.JrdsLogger;
import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.PartitionSpaceGraph;

import org.apache.log4j.Logger;
import org.snmp4j.smi.OID;


/**
 * @author bacchell
 *
 * TODO 
 */
public class PartitionSpace extends RdsIndexedSnmpRrd {
	static final private Logger logger = JrdsLogger.getLogger(PartitionSpace.class);
	static final private OID indexOid = new OID(".1.3.6.1.2.1.25.2.3.1.3");
	static final private OID allocUnitOid = new OID(".1.3.6.1.2.1.25.2.3.1.4");
	static final private OID totalOid = new OID(".1.3.6.1.2.1.25.2.3.1.5");
	static final private OID usedOid = new OID(".1.3.6.1.2.1.25.2.3.1.6");
	
	static final private ProbeDesc pd = new ProbeDesc(3);
	static {
		pd.add("Total", ProbeDesc.GAUGE, totalOid);
		pd.add("Used", ProbeDesc.GAUGE, usedOid);
		pd.add("hrStorageAllocationUnits", allocUnitOid);
		pd.setIndexOid(indexOid);
		pd.setGraphClasses(new Class[] {PartitionSpaceGraph.class});
		pd.setUniqIndex(true);
	}
	/**
	 * @param monitoredHost
	 * @param indexKey
	 */
	public PartitionSpace(RdsHost monitoredHost, String indexKey) {
		super(monitoredHost, pd, indexKey);
		setName(initName());
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.Probe#initName()
	 */
	protected String initName() {
		String retval = "fs-" + getIndexName();
		retval = retval.replace('\\', '_');
		retval = retval.replace(':', '_');
		retval = retval.replace('/', '_');
		return retval;
	}

	/**
	 * The want to store the value in octet, not in bloc size
	 * The translation is done by the probe, not the graph
	 * @see jrds.Probe#filterValues(java.util.Map)
	 */
	public Map filterValues(Map snmpVars) {
		int allocUnit = 0;
		long total = 0;
		long used = 0;
		for(Iterator i = snmpVars.entrySet().iterator(); i.hasNext();) {
			Map.Entry e = (Map.Entry) i.next();
			OID oid = (OID) e.getKey();
			Number value = (Number) e.getValue();
			oid.removeLast();
			if(allocUnitOid.equals(oid)) {
				allocUnit = value.intValue();
			}
			else if(totalOid.equals(oid)) {
				total = value.intValue();
			}
			else if(usedOid.equals(oid)) {
				used = value.intValue();
			}
		}
		total *= allocUnit;
		used *= allocUnit;
		Map retValue = new HashMap(2);
		retValue.put(totalOid, new Long(total));
		retValue.put(usedOid, new Long(used));
		
		return retValue;
	}
}
