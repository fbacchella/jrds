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
import jrds.graphe.UdpGraph;

import org.snmp4j.smi.OID;


/**
 * @author bacchell
 *
 */
public class UdpSnmp extends RdsSnmpSimple {
	static final private ProbeDesc pd = new ProbeDesc(4);
	static {
		pd.add("InDatagrams",ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.7.1"));
		pd.add("NoPorts",ProbeDesc.COUNTER,new OID(".1.3.6.1.2.1.7.2"));
		pd.add("InErrors",ProbeDesc.COUNTER,new OID(".1.3.6.1.2.1.7.3"));
		pd.add("OutDatagrams",ProbeDesc.COUNTER,new OID(".1.3.6.1.2.1.7.4"));
		pd.setRrdName("udp_snmp");
		pd.setGraphClasses(new Class[] {UdpGraph.class});
	}
	/**
	 * @param monitoredHost
	 */
	public UdpSnmp(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
