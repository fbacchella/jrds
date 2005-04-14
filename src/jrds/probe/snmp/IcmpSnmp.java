/*
 * Created on 7 déc. 2004
 *
 * TODO 
 */
package jrds.probe.snmp;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.RdsSnmpSimple;
import jrds.graphe.IcmpGraph;

import org.snmp4j.smi.OID;


/**
 * @author bacchell
 *
 * TODO 
 */
public class IcmpSnmp extends RdsSnmpSimple {

	static final ProbeDesc pd = new ProbeDesc(26);
	static {
		pd.add("InMsgs", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.5.1"));
		pd.add("InErrors", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.5.2"));
		pd.add("InDestUnreachs", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.5.3"));
		pd.add("InTimeExcds", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.5.4"));
		pd.add("InParmProbs", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.5.5"));
		pd.add("InSrcQuenchs", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.5.6"));
		pd.add("InRedirects", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.5.7"));
		pd.add("InEchos", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.5.8"));
		pd.add("InEchoReps", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.5.9"));
		pd.add("InTimestamps", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.5.10"));
		pd.add("InTimestampReps", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.5.11"));
		pd.add("InAddrMasks", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.5.12"));
		pd.add("InAddrMaskReps", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.5.13"));
		pd.add("OutMsgs", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.5.14"));
		pd.add("OutErrors", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.5.15"));
		pd.add("OutDestUnreachs", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.5.16"));
		pd.add("OutTimeExcds", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.5.17"));
		pd.add("OutParmProbs", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.5.18"));
		pd.add("OutSrcQuenchs", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.5.19"));
		pd.add("OutRedirects", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.5.20"));
		pd.add("OutEchos", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.5.21"));
		pd.add("OutEchoReps", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.5.22"));
		pd.add("OutTimestamps", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.5.23"));
		pd.add("OutTimestampReps", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.5.24"));
		pd.add("OutAddrMasks", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.5.25"));
		pd.add("OutAddrMaskReps", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.5.26"));
		pd.setRrdName("icmp");
		pd.setGraphClasses(new Object[] {"icmpsnmp.xml"});
	}

	/**
	 * @param monitoredHost
	 */
	public IcmpSnmp(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
