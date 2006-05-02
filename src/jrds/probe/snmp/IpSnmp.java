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
public class IpSnmp extends RdsSnmpSimple {
	static final private ProbeDesc pd = new ProbeDesc(8);
	static {
		pd.add("ipInReceives", DsType.COUNTER, new OID(".1.3.6.1.2.1.4.3"));
		pd.add("ipInHdrErrors", DsType.COUNTER, new OID(".1.3.6.1.2.1.4.4"));
		pd.add("ipInAddrErrors", DsType.COUNTER, new OID(".1.3.6.1.2.1.4.5"));
		pd.add("ipForwDatagrams", DsType.COUNTER, new OID(".1.3.6.1.2.1.4.6"));
		pd.add("ipInUnknownProtos", DsType.COUNTER, new OID(".1.3.6.1.2.1.4.7"));
		pd.add("ipInDiscards", DsType.COUNTER, new OID(".1.3.6.1.2.1.4.8"));
		pd.add("ipInDelivers", DsType.COUNTER, new OID(".1.3.6.1.2.1.4.9"));
		pd.add("ipOutRequests", DsType.COUNTER, new OID(".1.3.6.1.2.1.4.10"));
		pd.add("ipOutDiscards", DsType.COUNTER, new OID(".1.3.6.1.2.1.4.11"));
		pd.add("ipOutNoRoutes", DsType.COUNTER, new OID(".1.3.6.1.2.1.4.12"));
		pd.add("ipReasmReqds", DsType.COUNTER, new OID(".1.3.6.1.2.1.4.14"));
		pd.add("ipReasmOKs", DsType.COUNTER, new OID(".1.3.6.1.2.1.4.15"));
		pd.add("ipReasmFails", DsType.COUNTER, new OID(".1.3.6.1.2.1.4.16"));
		pd.add("ipFragOKs", DsType.COUNTER, new OID(".1.3.6.1.2.1.4.15"));
		pd.add("ipFragFails", DsType.COUNTER, new OID(".1.3.6.1.2.1.4.18"));
		pd.add("ipFragCreates", DsType.COUNTER, new OID(".1.3.6.1.2.1.4.19"));
		pd.add("ipRoutingDiscards", DsType.COUNTER, new OID(".1.3.6.1.2.1.4.23"));
		
		
		pd.setProbeName("ipsnmp");
		pd.setRequester(SnmpRequester.SIMPLE);
		
		pd.setGraphClasses(new Object[] { "ipactivitysnmp", "iperrors"});
	}
	
	/**
	 * @param monitoredHost
	 */
	public IpSnmp(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
