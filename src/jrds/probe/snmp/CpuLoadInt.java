/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds.probe.snmp;

import java.util.Iterator;
import java.util.Map;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.snmp.SnmpRequester;

import org.rrd4j.DsType;
import org.snmp4j.smi.OID;

/**
 * Used to get the load average, using the int value
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public class CpuLoadInt
extends RdsSnmpSimple {
	static final private ProbeDesc pd = new ProbeDesc(3);
	static {
		pd.add("la1", DsType.GAUGE, new OID(".1.3.6.1.4.1.2021.10.1.5.1"));
		pd.add("la5", DsType.GAUGE, new OID(".1.3.6.1.4.1.2021.10.1.5.2"));
		pd.add("la15", DsType.GAUGE, new OID(".1.3.6.1.4.1.2021.10.1.5.3"));
		pd.setProbeName("laverage");
		pd.setRequester(SnmpRequester.RAW);
		
		pd.setGraphClasses(new Object[] {"cpuload"});
	}
	
	/**
	 * simple constructor
	 *
	 * @param monitoredHost RdsHost
	 */
	public CpuLoadInt(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
	
	public Map filterValues(Map snmpVars) {
		for(Iterator i = snmpVars.entrySet().iterator(); i.hasNext();) {
			Map.Entry e = (Map.Entry) i.next();
			Number value = (Number) e.getValue();
			e.setValue(new Double(value.intValue()/100.0));
		}
		
		return snmpVars;
		
	}
}
