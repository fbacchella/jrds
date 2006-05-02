/*
 * Created on 29 d�c. 2004
 *
 * TODO 
 */
package jrds.probe.snmp;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.MemGraph;
import jrds.snmp.SnmpRequester;

import org.rrd4j.DsType;
import org.snmp4j.smi.OID;


/**
 * @author bacchell
 *
 * TODO 
 */
public class Mem extends RdsSnmpSimple {
	static final private ProbeDesc pd = new ProbeDesc(4);
	static {
		pd.add("memTotalSwap", DsType.GAUGE, new OID(".1.3.6.1.4.1.2021.4.3"));
		pd.add("memAvailSwap", DsType.GAUGE, new OID(".1.3.6.1.4.1.2021.4.4"));
		pd.add("memTotalReal", DsType.GAUGE, new OID(".1.3.6.1.4.1.2021.4.5"));
		pd.add("memAvailReal", DsType.GAUGE, new OID(".1.3.6.1.4.1.2021.4.6"));
		pd.setGraphClasses(new Class[] {MemGraph.class});
	       pd.setRequester(SnmpRequester.SIMPLE);
		pd.setProbeName("memory");
	}
	/**
	 * @param monitoredHost
	 */
	public Mem(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
