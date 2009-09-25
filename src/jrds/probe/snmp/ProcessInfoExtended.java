/*##########################################################################
_##
_##  $Id: ProcessInfo.java 321 2006-08-14 14:03:04 +0000 (lun., 14 août 2006) fbacchella $
_##
_##########################################################################*/

package jrds.probe.snmp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import jrds.snmp.SnmpVars;
import jrds.snmp.TabularIterator;

import org.apache.log4j.Logger;
import org.rrd4j.core.Sample;
import org.snmp4j.smi.OID;


/**
 * A class to probe info about a process, using MIB-II
 * @author Fabrice Bacchella 
 * @version $Revision: 321 $,  $Date: 2006-08-14 14:03:04 +0000 (lun., 14 août 2006) $
 */
public class ProcessInfoExtended extends RdsIndexedSnmpRrd {
	static final private Logger logger = Logger.getLogger(ProcessInfoExtended.class);
	static final private OID hrSWRunPath = new OID(".1.3.6.1.2.1.25.4.2.1.4");
	static final private OID hrSWRunParameters = new OID(".1.3.6.1.2.1.25.4.2.1.5");
	static final private OID hrSWRunPerfMem = new OID(".1.3.6.1.2.1.25.5.1.1.2");
	static final private OID  hrSWRunPerfCPU = new OID(".1.3.6.1.2.1.25.5.1.1.1");
	static final private String MIN = "Minimum";
	static final private String MAX = "Maximum";
	static final private String AVERAGE = "Average";
	static final private String NUM = "Number";
	static final private String CPU = "Cpu";

	private String pattern;

	/**
	 * @param monitoredHost
	 */
	public boolean configure(String indexName, String pattern)
	{
		this.pattern = pattern;
		return super.configure(indexName);
	}

	@Override
	public Collection<OID> getIndexSet()
	{
		Collection<OID> indexes = new HashSet<OID>(2);
		indexes.add(hrSWRunPath);
		indexes.add(hrSWRunParameters);
		return indexes;
	}

	/* (non-Javadoc)
	 * @see jrds.probe.snmp.RdsIndexedSnmpRrd#readSpecific()
	 */
	@Override
	public boolean readSpecific() {
		getPd().addSpecific(RdsIndexedSnmpRrd.INDEXOIDNAME, hrSWRunPath.toString());
		return super.readSpecific();
	}

	@SuppressWarnings("unchecked")
	public Collection<int[]> setIndexValue() 
	{

		boolean found = false;
		Collection<OID> soidSet= getIndexSet();
		Pattern p = Pattern.compile(pattern);

		Collection<int[]>  indexAsString = new HashSet<int[]>();
		TabularIterator ti = new TabularIterator(getSnmpStarter(), soidSet);
		for(SnmpVars s: ti) {
			List<OID> lk = new ArrayList<OID>(s.keySet());
			Collections.sort(lk);
			StringBuffer cmdBuf = new StringBuffer();
			for(OID oid: lk) {
				cmdBuf.append(s.get(oid));
				cmdBuf.append(" ");
			}
			if(p.matcher(cmdBuf.toString().trim()).matches()) {
				int[] index = new int[1];
				index[0] = lk.get(0).last();
				indexAsString.add(index);
				found = true;
			}
		}
		if(! found) {
			logger.error("index for " + indexKey + " not found for host " + getHost().getName());
			indexAsString = null;
		}
		return indexAsString;
	}

	/* (non-Javadoc)
	 * @see jrds.Probe#modifySample(org.rrd4j.core.Sample, java.util.Map)
	 */
	@Override
	public void modifySample(Sample oneSample, Map<OID, Object> snmpVars) {
		double max = 0;
		double min = Double.MAX_VALUE;
		double average = 0;
		int nbvalue = 0;
		double cpuUsed = 0;
		for(Map.Entry<OID, Object> e: ((Map<OID, Object>)snmpVars).entrySet()) {
			OID oid = e.getKey();
			if(oid.startsWith(hrSWRunPerfMem)) {
				double value = ((Number)e.getValue()).doubleValue() * 1024;
				max = Math.max(max, value);
				min = Math.min(min, value);
				average += value;
				nbvalue++;
			}
			else if(oid.startsWith(hrSWRunPerfCPU)) {
				cpuUsed += ((Number)e.getValue()).doubleValue() / 100.0;
			}
		}
		average /= nbvalue;
		oneSample.setValue(NUM, nbvalue);
		oneSample.setValue(MAX, max);
		oneSample.setValue(MIN, min);
		oneSample.setValue(AVERAGE, average);
		oneSample.setValue(CPU, cpuUsed);		
	}
}

