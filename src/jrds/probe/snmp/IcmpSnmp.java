/*
 * Created on 7 déc. 2004
 *
 * TODO 
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
 * TODO 
 */
public class IcmpSnmp extends RdsSnmpSimple {

	static final ProbeDesc pd = new ProbeDesc(26);
	static {
		pd.add("InMsgs", DsType.COUNTER, new OID(".1.3.6.1.2.1.5.1"));
		pd.add("InErrors", DsType.COUNTER, new OID(".1.3.6.1.2.1.5.2"));
		pd.add("InDestUnreachs", DsType.COUNTER, new OID(".1.3.6.1.2.1.5.3"));
		pd.add("InTimeExcds", DsType.COUNTER, new OID(".1.3.6.1.2.1.5.4"));
		pd.add("InParmProbs", DsType.COUNTER, new OID(".1.3.6.1.2.1.5.5"));
		pd.add("InSrcQuenchs", DsType.COUNTER, new OID(".1.3.6.1.2.1.5.6"));
		pd.add("InRedirects", DsType.COUNTER, new OID(".1.3.6.1.2.1.5.7"));
		pd.add("InEchos", DsType.COUNTER, new OID(".1.3.6.1.2.1.5.8"));
		pd.add("InEchoReps", DsType.COUNTER, new OID(".1.3.6.1.2.1.5.9"));
		pd.add("InTimestamps", DsType.COUNTER, new OID(".1.3.6.1.2.1.5.10"));
		pd.add("InTimestampReps", DsType.COUNTER, new OID(".1.3.6.1.2.1.5.11"));
		pd.add("InAddrMasks", DsType.COUNTER, new OID(".1.3.6.1.2.1.5.12"));
		pd.add("InAddrMaskReps", DsType.COUNTER, new OID(".1.3.6.1.2.1.5.13"));
		pd.add("OutMsgs", DsType.COUNTER, new OID(".1.3.6.1.2.1.5.14"));
		pd.add("OutErrors", DsType.COUNTER, new OID(".1.3.6.1.2.1.5.15"));
		pd.add("OutDestUnreachs", DsType.COUNTER, new OID(".1.3.6.1.2.1.5.16"));
		pd.add("OutTimeExcds", DsType.COUNTER, new OID(".1.3.6.1.2.1.5.17"));
		pd.add("OutParmProbs", DsType.COUNTER, new OID(".1.3.6.1.2.1.5.18"));
		pd.add("OutSrcQuenchs", DsType.COUNTER, new OID(".1.3.6.1.2.1.5.19"));
		pd.add("OutRedirects", DsType.COUNTER, new OID(".1.3.6.1.2.1.5.20"));
		pd.add("OutEchos", DsType.COUNTER, new OID(".1.3.6.1.2.1.5.21"));
		pd.add("OutEchoReps", DsType.COUNTER, new OID(".1.3.6.1.2.1.5.22"));
		pd.add("OutTimestamps", DsType.COUNTER, new OID(".1.3.6.1.2.1.5.23"));
		pd.add("OutTimestampReps", DsType.COUNTER, new OID(".1.3.6.1.2.1.5.24"));
		pd.add("OutAddrMasks", DsType.COUNTER, new OID(".1.3.6.1.2.1.5.25"));
		pd.add("OutAddrMaskReps", DsType.COUNTER, new OID(".1.3.6.1.2.1.5.26"));
		pd.setProbeName("icmp");
		pd.setGraphClasses(new Object[] {"icmpsnmp"});
        pd.setRequester(SnmpRequester.SIMPLE);
	}

	/**
	 * @param monitoredHost
	 */
	public IcmpSnmp(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
