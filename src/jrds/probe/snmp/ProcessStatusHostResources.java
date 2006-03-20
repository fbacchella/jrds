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
import jrds.snmp.SnmpRequester;

import org.apache.log4j.Logger;
import org.snmp4j.smi.OID;


/**
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class ProcessStatusHostResources extends RdsSnmpSimple {
	static final private Logger logger = Logger.getLogger(ProcessStatusHostResources.class);
	static final private OID psProcessState = new OID(".1.3.6.1.2.1.25.4.2.1.7");
	static final private String RUNNING="running";
	static final private int RUNNINGINDEX = 1;
	static final private String RUNNABLE="runnable";
	static final private int RUNNABLEINDEX = 2;
	static final private String NOTRUNNABLE="notRunnable";
	static final private int NOTRUNNABLEINDEX = 3;
	static final private String INVALID="invalid";
	static final private int INVALIDINDEX = 4;
	
	static final private ProbeDesc pd = new ProbeDesc(7);
	static {
		pd.add("psProcessState", psProcessState);
		pd.add(RUNNING, ProbeDesc.GAUGE);
		pd.add(RUNNABLE, ProbeDesc.GAUGE);
		pd.add(NOTRUNNABLE, ProbeDesc.GAUGE);
		pd.add(INVALID, ProbeDesc.GAUGE);
		pd.setGraphClasses(new Object []{"processstatushostsresources.xml"});
		pd.setProbeName("hrpslist");
		pd.setRequester(SnmpRequester.TABULAR);
	}
	
	/**
	 * @param monitoredHost
	 */
	public ProcessStatusHostResources(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}

	/**
	 * @see jrds.Probe#filterValues(java.util.Map)
	 */
	public Map filterValues(Map snmpVars){
		int running = 0;
		int runnable = 0;
		int notRunnable = 0;
		int invalid = 0;
		for(Iterator i = snmpVars.keySet().iterator() ; i.hasNext() ; ){
			OID oid = (OID) i.next();
			int state = ((Number) snmpVars.get(oid)).intValue();
			if(RUNNINGINDEX == state)
				running++;
			else if(RUNNABLEINDEX == state)
				runnable++;
			else if(NOTRUNNABLEINDEX == state)
				notRunnable++;
			else if(INVALIDINDEX == state)
				invalid++;
			
		}
		Map retValue = new HashMap(7);
		retValue.put(RUNNING, new Double(running));
		retValue.put(RUNNABLE, new Double(runnable));
		retValue.put(NOTRUNNABLE, new Double(notRunnable));
		retValue.put(INVALID, new Double(invalid));
		return retValue;
	}
	
}
