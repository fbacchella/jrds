/*
 * Created on 2 déc. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.probe.snmp;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.snmp.SnmpRequester;

import org.rrd4j.DsType;
import org.snmp4j.smi.OID;


/**
 * @author bacchell
 *
 */
public class TcpSnmp extends RdsSnmpSimple {
	static final private ProbeDesc pd = new ProbeDesc(8);
	static {
		pd.add("ActiveOpens", DsType.COUNTER, new OID(".1.3.6.1.2.1.6.5"));
		pd.add("PassiveOpens", DsType.COUNTER, new OID(".1.3.6.1.2.1.6.6"));
		pd.add("AttemptFails", DsType.COUNTER, new OID(".1.3.6.1.2.1.6.7"));
		pd.add("EstabResets", DsType.COUNTER, new OID(".1.3.6.1.2.1.6.8"));
		pd.add("CurrEstab", DsType.GAUGE, new OID(".1.3.6.1.2.1.6.9"));
		pd.add("InSegs", DsType.COUNTER, new OID(".1.3.6.1.2.1.6.10"));
		pd.add("OutSegs", DsType.COUNTER, new OID(".1.3.6.1.2.1.6.11"));
		pd.add("RetransSegs", DsType.COUNTER, new OID(".1.3.6.1.2.1.6.12"));
		pd.setRequester(SnmpRequester.SIMPLE);

		pd.setProbeName("tcp_snmp");
		pd.setGraphClasses(new Object[] {"tcpconnection", "tcpestablished", "tcpsegments"});
	}

	/**
	 * @param monitoredHost
	 */
	public TcpSnmp(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
