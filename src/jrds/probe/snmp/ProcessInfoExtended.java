/*##########################################################################
_##
_##  $Id: ProcessInfo.java 321 2006-08-14 14:03:04 +0000 (lun., 14 août 2006) fbacchella $
_##
_##########################################################################*/

package jrds.probe.snmp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import jrds.snmp.SnmpVars;
import jrds.snmp.TabularIterator;

import org.apache.log4j.Logger;
import org.snmp4j.smi.OID;


/**
 * A class to probe info about a process, using MIB-II
 * @author Fabrice Bacchella 
 * @version $Revision: 321 $,  $Date: 2006-08-14 14:03:04 +0000 (lun., 14 août 2006) $
 */
public class ProcessInfoExtended extends RdsIndexedSnmpRrd {
	static final private Logger logger = Logger.getLogger(ProcessInfo.class);
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
	public ProcessInfoExtended(String indexName, String pattern)
	{
		super(indexName);
		setName("psx-" + indexName);
		this.pattern = pattern;
	}

	@Override
	public Collection<OID> getIndexSet()
	{
		Collection<OID> indexes = new HashSet<OID>(2);
		indexes.add(hrSWRunPath);
		indexes.add(hrSWRunParameters);
		return indexes;
	}

	@SuppressWarnings("unchecked")
	public Collection<String> setIndexValue() 
	{

		boolean found = false;
		Collection<OID> soidSet= getIndexSet();
		Pattern p = Pattern.compile(pattern);

		Collection<String>  indexAsString = new HashSet<String>();
		for(TabularIterator i = new TabularIterator(this, soidSet) ; i.hasNext() ; ) {
			SnmpVars s = i.next();
			List<OID> lk = new ArrayList<OID>(s.keySet());
			Collections.sort(lk);
			StringBuffer cmdBuf = new StringBuffer();
			for(OID oid: lk) {
				cmdBuf.append(s.get(oid));
				cmdBuf.append(" ");
			}
			if(p.matcher(cmdBuf.toString().trim()).matches()) {
				int index = lk.get(0).last();
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
	 * @see jrds.Probe#filterValues(java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	public Map<?, Number> filterValues(Map snmpVars)
	{
		double max = 0;
		double min = Double.MAX_VALUE;
		double average = 0;
		int nbvalue = 0;
		double cpuUsed = 0;
		for(Map.Entry<OID, Number> e: ((Map<OID, Number>)snmpVars).entrySet()) {
			OID oid = e.getKey();
			if(oid.startsWith(hrSWRunPerfMem)) {
				double value = e.getValue().doubleValue() * 1024;
				max = Math.max(max, value);
				min = Math.min(min, value);
				average += value;
				nbvalue++;
			}
			else if(oid.startsWith(hrSWRunPerfCPU)) {
				cpuUsed += e.getValue().doubleValue() / 100.0;
			}
		}
		average /= nbvalue;
		Map<String, Number> retValue = new HashMap<String, Number>(5);
		retValue.put(NUM, nbvalue);
		retValue.put(MAX, max);
		retValue.put(MIN, min);
		retValue.put(AVERAGE, average);
		retValue.put(CPU, cpuUsed);		
		return retValue;
	}
}

