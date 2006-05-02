/*
 * Created on 3 févr. 2005
 *
 * TODO 
 */
package jrds.probe.snmp;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.VMSolarisGraph;
import jrds.snmp.SnmpRequester;

import org.rrd4j.DsType;
import org.snmp4j.smi.OID;


/**
 * @author bacchell
 *
 * TODO 
 */
public final class VMSolaris extends RdsSnmpSimple {
	static final private ProbeDesc pd = new ProbeDesc(4);
	static {
		pd.add("rsVPagesIn", DsType.COUNTER, new OID(".1.3.6.1.4.1.42.3.13.9"));
		pd.add("rsVPagesOut", DsType.COUNTER, new OID(".1.3.6.1.4.1.42.3.13.10"));
		pd.add("rsVSwapIn", DsType.COUNTER, new OID(".1.3.6.1.4.1.42.3.13.11"));
		pd.add("rsVSwapOut", DsType.COUNTER, new OID(".1.3.6.1.4.1.42.3.13.12"));
		pd.setProbeName("vmsolaris");
		pd.setGraphClasses(new Class[] {VMSolarisGraph.class});
		pd.setRequester(SnmpRequester.SIMPLE);
	}
	
	/**
	 * @param monitoredHost
	 */
	public VMSolaris(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
