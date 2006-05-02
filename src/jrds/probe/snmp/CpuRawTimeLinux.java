/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds.probe.snmp;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.CpuRawTimeLinuxGraph;
import jrds.snmp.SnmpRequester;

import org.rrd4j.DsType;
import org.snmp4j.smi.OID;


/**
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class CpuRawTimeLinux extends RdsSnmpSimple {
	static final private ProbeDesc pd = new ProbeDesc(7);
	static {
		pd.add("ssCpuRawUser", DsType.COUNTER, new OID(".1.3.6.1.4.1.2021.11.50")); 
		pd.add("ssCpuRawNice", DsType.COUNTER, new OID(".1.3.6.1.4.1.2021.11.51"));
		pd.add("ssCpuRawSystem", DsType.COUNTER, new OID(".1.3.6.1.4.1.2021.11.52"));
		pd.add("ssCpuRawIdle", DsType.COUNTER, new OID(".1.3.6.1.4.1.2021.11.53"));
		pd.add("ssCpuRawWait", DsType.COUNTER, new OID(".1.3.6.1.4.1.2021.11.54"));
		pd.add("ssCpuRawKernel", DsType.COUNTER, new OID(".1.3.6.1.4.1.2021.11.55"));
		pd.add("ssCpuRawInterrupt", DsType.COUNTER, new OID(".1.3.6.1.4.1.2021.11.56"));
		pd.setProbeName("cpurawlinux");
		pd.setGraphClasses(new Object[] {CpuRawTimeLinuxGraph.class, "cpurawkilinux"});
		pd.setRequester(SnmpRequester.SIMPLE);
	}
	
	/**
	 * @param monitoredHost
	 */
	public CpuRawTimeLinux(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
