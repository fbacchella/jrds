/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds.probe.snmp;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.CpuTimeSunMibGraph;
import jrds.snmp.SnmpRequester;

import org.rrd4j.DsType;
import org.snmp4j.smi.OID;


/**
 * A class to probe the CPU load on a cisco
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class CpuTimeSunMib extends RdsSnmpSimple {
	static final private ProbeDesc pd = new ProbeDesc(4);
	static {
		pd.add("rsUserProcessTime", DsType.COUNTER, new OID(".1.3.6.1.4.1.42.3.13.1"));
		pd.add("rsNiceModeTime", DsType.COUNTER, new OID(".1.3.6.1.4.1.42.3.13.2"));
		pd.add("rsSystemProcessTime", DsType.COUNTER, new OID(".1.3.6.1.4.1.42.3.13.3"));
		pd.add("rsIdleModeTime", DsType.COUNTER, new OID(".1.3.6.1.4.1.42.3.13.4"));
		pd.setProbeName("cpusunmib");
		pd.setGraphClasses(new Class[] {CpuTimeSunMibGraph.class});
        pd.setRequester(SnmpRequester.SIMPLE);
	}

	/**
	 * @param monitoredHost
	 */
	public CpuTimeSunMib(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
