/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds.probe.snmp;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.CpuRawTimeSolarisGraph;
import jrds.snmp.SnmpRequester;

import org.snmp4j.smi.OID;


/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class CpuRawTimeSolaris extends RdsSnmpSimple {

	static final ProbeDesc pd = new ProbeDesc(4);
	static {
		pd.add("ssCpuRawUser", ProbeDesc.COUNTER, new OID(".1.3.6.1.4.1.2021.11.50"));
		pd.add("ssCpuRawIdle", ProbeDesc.COUNTER, new OID(".1.3.6.1.4.1.2021.11.53"));
		pd.add("ssCpuRawWait", ProbeDesc.COUNTER, new OID(".1.3.6.1.4.1.2021.11.54"));
		pd.add("ssCpuRawKernel", ProbeDesc.COUNTER, new OID(".1.3.6.1.4.1.2021.11.55"));
		pd.setName("cpurawsol");
		pd.setGraphClasses(new Class[] {CpuRawTimeSolarisGraph.class});
        pd.setRequester(SnmpRequester.SIMPLE);
	}
	/**
	 * @param monitoredHost
	 */
	public CpuRawTimeSolaris(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
