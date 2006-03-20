/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds.probe.snmp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.ProcessInfoNumber;
import jrds.graphe.ProcessInfoSize;

import org.apache.log4j.Logger;
import org.snmp4j.smi.OID;


/**
 * A class to probe info about a process, using the Solaris MIB
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class ProcessInfoSolaris extends RdsIndexedSnmpRrd {
	static final private Logger logger = Logger.getLogger(RdsIndexedSnmpRrd.class);
	static final private OID psProcessSize = new OID(".1.3.6.1.4.1.42.3.12.1.3");
	static final private OID  psProcessCpuTime = new OID(".1.3.6.1.4.1.42.3.12.1.4");
	static final private OID indexOid = new OID(".1.3.6.1.4.1.42.3.12.1.10");
	static final private String MIN = "Minimum";
	static final private String MAX = "Maximum";
	static final private String AVERAGE = "Average";
	static final private String NUM = "Number";
	static final private ProbeDesc pd = new ProbeDesc(2);
	static {
		pd.add("psProcessSize", psProcessSize);
		pd.add("psProcessCpuTime", psProcessCpuTime);
		pd.add(MIN, ProbeDesc.GAUGE);
		pd.add(MAX, ProbeDesc.GAUGE);
		pd.add(AVERAGE, ProbeDesc.GAUGE);
		pd.add(NUM, ProbeDesc.GAUGE);
		pd.setGraphClasses(new Class[] {ProcessInfoNumber.class, ProcessInfoSize.class});
		pd.setIndexOid(indexOid);
		pd.setProbeName("ps-{1}");
		pd.setUniqIndex(false);
	}
	
	/**
	 * @param monitoredHost
	 * @param indexName
	 */
	public ProcessInfoSolaris(RdsHost monitoredHost, String indexName) {
		super(monitoredHost, pd, indexName);
	}
	
	/**
	 * @see jrds.Probe#filterValues(java.util.Map)
	 */
	public Map filterValues(Map snmpVars)  {
		double max = 0;
		double min = Double.MAX_VALUE;
		double average = 0;
		int nbvalue = 0;
		for(Iterator i = snmpVars.keySet().iterator() ; i.hasNext() ; ){
			OID oid = (OID) i.next();
			double value = ((Number)snmpVars.get(oid)).doubleValue() * 1024;
			if(oid.startsWith(psProcessSize)) {
				max = Math.max(max, value) * 1024;
				min = Math.min(min, value) * 1024;
				average += value;
				nbvalue++;
			}
		}
		average /= nbvalue;
		Map retValue = new HashMap(4);
		retValue.put(NUM, new Double(nbvalue));
		retValue.put(MAX, new Double(max));
		retValue.put(MIN, new Double(max));
		retValue.put(AVERAGE, new Double(average));
		return retValue;
	}
}
