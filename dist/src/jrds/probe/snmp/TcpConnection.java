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
import jrds.graphe.TcpConnectionGraph;

import org.snmp4j.smi.OID;


/**
 * @author bacchell
 *
 */
public class TcpConnection extends RdsSnmpSimple {
	private static final ProbeDesc pd = new ProbeDesc(8);
	static {
		pd.add("ActiveOpens", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.6.5"));
		pd.add("PassiveOpens", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.6.6"));
		pd.add("AttemptFails", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.6.7"));
		pd.add("EstabResets", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.6.8"));
		pd.add("CurrEstab", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.6.9"));
		pd.add("InSegs", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.6.10"));
		pd.add("OutSegs", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.6.11"));
		pd.add("RetransSegs", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.6.12"));
		pd.setRrdName("tcp");
		pd.setGraphClasses(new Class[] {TcpConnectionGraph.class});
		
	}
	
	/**
	 * @param monitoredHost
	 */
	public TcpConnection(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
