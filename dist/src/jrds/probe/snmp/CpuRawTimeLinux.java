/*
 * Created on 2 d�c. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.probe.snmp;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.RdsSnmpSimple;
import jrds.graphe.CpuRawKILinuxGraph;
import jrds.graphe.CpuRawTimeLinuxGraph;

import org.snmp4j.smi.OID;


/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class CpuRawTimeLinux extends RdsSnmpSimple {
	static final private ProbeDesc pd = new ProbeDesc(7);
	static {
		pd.add("ssCpuRawUser", ProbeDesc.COUNTER, new OID(".1.3.6.1.4.1.2021.11.50")); 
		pd.add("ssCpuRawNice", ProbeDesc.COUNTER, new OID(".1.3.6.1.4.1.2021.11.51"));
		pd.add("ssCpuRawSystem", ProbeDesc.COUNTER, new OID(".1.3.6.1.4.1.2021.11.52"));
		pd.add("ssCpuRawIdle", ProbeDesc.COUNTER, new OID(".1.3.6.1.4.1.2021.11.53"));
		pd.add("ssCpuRawWait", ProbeDesc.COUNTER, new OID(".1.3.6.1.4.1.2021.11.54"));
		pd.add("ssCpuRawKernel", ProbeDesc.COUNTER, new OID(".1.3.6.1.4.1.2021.11.55"));
		pd.add("ssCpuRawInterrupt", ProbeDesc.COUNTER, new OID(".1.3.6.1.4.1.2021.11.56"));
		pd.setRrdName("cpurawlinux");
		pd.setGraphClasses(new Class[] {CpuRawTimeLinuxGraph.class, CpuRawKILinuxGraph.class});
	}

	/**
	 * @param monitoredHost
	 */
	public CpuRawTimeLinux(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
