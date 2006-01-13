/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds.probe.snmp;

import java.util.Map;

import jrds.JrdsLogger;
import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.RdsSnmpSimple;
import jrds.snmp.SnmpRequester;

import org.apache.log4j.Logger;
import org.snmp4j.smi.OID;

/**
 * Used to get the load average, using the float value
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class CpuLoadFloat
extends RdsSnmpSimple {
	static final private Logger logger = JrdsLogger.getLogger(CpuLoadFloat.class);
	
	static final private ProbeDesc pd = new ProbeDesc(3);
	static {
		pd.add("la1", ProbeDesc.GAUGE, new OID(".1.3.6.1.4.1.2021.10.1.6.1"));
		pd.add("la5", ProbeDesc.GAUGE, new OID(".1.3.6.1.4.1.2021.10.1.6.2"));
		pd.add("la15", ProbeDesc.GAUGE, new OID(".1.3.6.1.4.1.2021.10.1.6.3"));
		pd.setRrdName("laverage");
		pd.setRequester(SnmpRequester.RAW);
		
		pd.setGraphClasses(new Object[] {"cpuload.xml"});
	}
	
	/**
	 * simple constructor
	 *
	 * @param monitoredHost RdsHost
	 */
	public CpuLoadFloat(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
	
	/**
	 * For this probe, raw result is used
	 * @see jrds.Probe#filterValues(java.util.Map)
	 */
	public Map filterValues(Map snmpVars) {
		return snmpVars;
		
	}
}
