/*
 * Created on 2 déc. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.probe.snmp;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.CpuTimeSunMibGraph;
import jrds.snmp.SnmpRequester;

import org.snmp4j.smi.OID;


/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class CpuTimeSunMib extends RdsSnmpSimple {
	static final private ProbeDesc pd = new ProbeDesc(4);
	static {
		pd.add("rsUserProcessTime", ProbeDesc.COUNTER, new OID(".1.3.6.1.4.1.42.3.13.1"));
		pd.add("rsNiceModeTime", ProbeDesc.COUNTER, new OID(".1.3.6.1.4.1.42.3.13.2"));
		pd.add("rsSystemProcessTime", ProbeDesc.COUNTER, new OID(".1.3.6.1.4.1.42.3.13.3"));
		pd.add("rsIdleModeTime", ProbeDesc.COUNTER, new OID(".1.3.6.1.4.1.42.3.13.4"));
		pd.setRrdName("cpusunmib");
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
