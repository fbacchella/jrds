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
import jrds.graphe.CiscoCpuGraph;

import org.snmp4j.smi.OID;


/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class CiscoCpu extends RdsSnmpSimple {
	static final private ProbeDesc pd = new ProbeDesc(2);
	static {
		pd.add("la1", ProbeDesc.GAUGE, new OID(".1.3.6.1.4.1.9.2.1.57"));
		pd.add("la5", ProbeDesc.GAUGE, new OID(".1.3.6.1.4.1.9.2.1.58"));
		pd.setRrdName("ciscocpuload");
		pd.setGraphClasses(new Class[] {CiscoCpuGraph.class});
	}

	/**
	 * @param monitoredHost
	 */
	public CiscoCpu(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}

}
