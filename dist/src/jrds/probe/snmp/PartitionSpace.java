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
import jrds.RdsIndexedSnmpRrd;
import jrds.graphe.PartitionSpaceGraph;
import jrds.snmp.SnmpVars;

import org.apache.log4j.Logger;
import org.snmp4j.smi.OID;


/**
 * @author bacchell
 *
 * TODO 
 */
public class PartitionSpace extends RdsIndexedSnmpRrd {
	static final private Logger logger = JrdsLogger.getLogger(PartitionSpace.class.getPackage().getName());
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
	}
	/**
	 * @param monitoredHost
	 * @param indexKey
	 */
	public PartitionSpace(RdsHost monitoredHost, String indexKey) {
		super(monitoredHost, pd, indexKey);
		setRrdName(initName());
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsIndexedSnmpRrd#initIsUniq()
	 */
	protected boolean initIsUniq() {
		return true;
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

	public Map filterValues(SnmpVars snmpVars) {
		int allocUnit = 0;
		long total = 0;
		long used = 0;
		for(Iterator i = snmpVars.entrySet().iterator(); i.hasNext();) {
			Map.Entry e = (Map.Entry) i.next();
			OID oid = (OID) e.getKey();
			Number value = (Number) e.getValue();    //SnmpVars.convertVar((Variable) e.getValue());
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
	
	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsSnmpRrd#storeValues(com.aol.jrds.snmp.SnmpVars, org.jrobin.core.Sample)
	 */
//	public void storeValues(SnmpVars snmpVars, Sample oneSample)
//	throws RrdException {
//		if(snmpVars != null && oneSample != null) {
//			int allocUnit = 0;
//			long total = 0;
//			long used = 0;
//			for(Iterator i = snmpVars.entrySet().iterator(); i.hasNext();) {
//				Map.Entry e = (Map.Entry) i.next();
//				OID oid = (OID) e.getKey();
//				Number value = (Number) e.getValue();    //SnmpVars.convertVar((Variable) e.getValue());
//				oid.removeLast();
//				if(allocUnitOid.equals(oid)) {
//					allocUnit = value.intValue();
//				}
//				else if(totalOid.equals(oid)) {
//					total = value.intValue();
//				}
//				else if(usedOid.equals(oid)) {
//					used = value.intValue();
//				}
//			}
//			
//			total *= allocUnit;
//			oneSample.setValue("Total", total);
//			used *= allocUnit;
//			oneSample.setValue("Used", used);
//		}
//	}
}
