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
public class UdpSnmp extends RdsSnmpSimple {
	static final private ProbeDesc pd = new ProbeDesc(4);
	static {
		pd.add("InDatagrams",DsType.COUNTER, new OID(".1.3.6.1.2.1.7.1"));
		pd.add("NoPorts",DsType.COUNTER,new OID(".1.3.6.1.2.1.7.2"));
		pd.add("InErrors",DsType.COUNTER,new OID(".1.3.6.1.2.1.7.3"));
		pd.add("OutDatagrams",DsType.COUNTER,new OID(".1.3.6.1.2.1.7.4"));
		pd.setProbeName("udp_snmp");
		pd.setGraphClasses(new Object[] {"udpactivity"});
		pd.setRequester(SnmpRequester.SIMPLE);
	}
	/**
	 * @param monitoredHost
	 */
	public UdpSnmp(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
