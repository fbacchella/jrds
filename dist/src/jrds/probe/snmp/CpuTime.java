/*
 * Created on 2 déc. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.probe.snmp;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.RdsSnmpSimple;
import jrds.graphe.CpuTimeGraph;

import org.snmp4j.smi.OID;


/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class CpuTime extends RdsSnmpSimple {

	static final ProbeDesc pd = new ProbeDesc(5);
	static {
		pd.add("ssCpuUser", ProbeDesc.GAUGE, new OID(".1.3.6.1.4.1.2021.11.9"));
		pd.add("ssCpuSystem", ProbeDesc.GAUGE, new OID(".1.3.6.1.4.1.2021.11.10"));
		pd.add("ssCpuIdle", ProbeDesc.GAUGE, new OID(".1.3.6.1.4.1.2021.11.11"));
		pd.add("NumProcess", ProbeDesc.GAUGE, new OID(".1.3.6.1.2.1.25.1.6."));
		pd.add("NumUsers", ProbeDesc.GAUGE, new OID(".1.3.6.1.2.1.25.1.5"));
		pd.setRrdName("cpu");
		pd.setGraphClasses(new Class[] {CpuTimeGraph.class});
	}
	/**
	 * @param monitoredHost
	 */
	public CpuTime(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}

}
