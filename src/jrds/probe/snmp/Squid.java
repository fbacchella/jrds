/*
 * Created on 24 janv. 2005
 *
 * TODO 
 */
package jrds.probe.snmp;

import jrds.GraphDesc;
import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.SquidBytesGraph;
import jrds.graphe.SquidCpu;
import jrds.graphe.SquidHitRatioGraph;
import jrds.graphe.SquidReqGraph;
import jrds.snmp.SnmpRequester;

import org.snmp4j.smi.OID;


/**
 * @author bacchell
 *
 * TODO 
 */
public class Squid extends RdsSnmpSimple {
	static final private ProbeDesc pd = new ProbeDesc(44);
	static {
		pd.add("SysPageFaults", ProbeDesc.COUNTER, new OID(".1.3.6.1.4.1.3495.1.3.1.1"));
		pd.add("SysNumReads", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.1.2"));
		pd.add("DnsNumberServers",ProbeDesc.GAUGE,new OID(".1.3.6.1.4.1.3495.1.4.3.3"));
		pd.add("DnsReplies", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.4.3.2"), 0, 10000);
		pd.add("DnsRequests", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.4.3.1"), 0, 10000);
		pd.add("BlkGetHostByAddr", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.4.2.7"));
		pd.add("FqdnMisses", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.4.2.6"));
		pd.add("FqdnNegativeHits", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.4.2.5"));
		pd.add("FqdnPendingHits", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.4.2.4"));
		pd.add("FqdnHits", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.4.2.3"), 0, 10000);
		pd.add("FqdnRequests", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.4.2.2"), 0, 10000);
		pd.add("FqdnEntries",ProbeDesc.GAUGE,new OID(".1.3.6.1.4.1.3495.1.4.2.1"), 0, 10000);
		pd.add("AttemptReleaseLckEnt", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.4.1.8"));
		pd.add("BlkGetHostByName", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.4.1.7"));
		pd.add("IpMisses",ProbeDesc.GAUGE,new OID(".1.3.6.1.4.1.3495.1.4.1.6"), 0, 10000);
		pd.add("IpNegativeHits",ProbeDesc.GAUGE,new OID(".1.3.6.1.4.1.3495.1.4.1.5"), 0, 10000);
		pd.add("IpPendingHits", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.4.1.4"), 0, 10000);
		pd.add("IpHits", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.4.1.3"), 0, 10000);
		pd.add("IpRequests", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.4.1.2"), 0, 10000);
		pd.add("IpEntries",ProbeDesc.GAUGE,new OID(".1.3.6.1.4.1.3495.1.4.1.1"));
		pd.add("Clients", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.15"));
		pd.add("CurrentSwapSize",ProbeDesc.GAUGE,new OID(".1.3.6.1.4.1.3495.1.3.2.1.14"));
		pd.add("ServerOutKb", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.13"), 0, 100000);
		pd.add("ServerInKb", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.12"), 0, 100000);
		pd.add("ServerErrors", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.11"), 0, 10000);
		pd.add("ServerRequests", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.10"), 0, 10000);
		pd.add("IcpKbRecv", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.9"), 0, 100000);
		pd.add("IcpKbSent", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.8"), 0, 100000);
		pd.add("IcpPktsRecv", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.7"));
		pd.add("IcpPktsSent", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.6"));
		pd.add("HttpOutKb", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.5"), 0, 100000);
		pd.add("HttpInKb", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.4"), 0, 100000);
		pd.add("HttpErrors", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.3"), 0, 10000);
		pd.add("HttpHits", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.2"), 0, 10000);
		pd.add("HttpRqt", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.1"), 0, 10000);
		pd.add("CurrentResFileDC",ProbeDesc.GAUGE,new OID(".1.3.6.1.4.1.3495.1.3.1.11"));
		pd.add("CurrentUnusedFDC",ProbeDesc.GAUGE,new OID(".1.3.6.1.4.1.3495.1.3.1.10"));
		pd.add("CurrentUnlinkRe",ProbeDesc.GAUGE,new OID(".1.3.6.1.4.1.3495.1.3.1.9"));
		pd.add("CurrentLRUExp", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.1.8"));
		pd.add("NumObjCount",ProbeDesc.GAUGE,new OID(".1.3.6.1.4.1.3495.1.3.1.7"));
		pd.add("MaxResSize",ProbeDesc.GAUGE,new OID(".1.3.6.1.4.1.3495.1.3.1.6"));
		pd.add("CpuUsage",ProbeDesc.GAUGE,new OID(".1.3.6.1.4.1.3495.1.3.1.5"));
		pd.add("CpuTime", ProbeDesc.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.1.4"));
		pd.add("MemUsage",ProbeDesc.GAUGE,new OID(".1.3.6.1.4.1.3495.1.3.1.3"));
		
		pd.setRequester(SnmpRequester.SIMPLE);
		
		//An associated graph for the number of object
		GraphDesc ipGraph = new GraphDesc(1);
		ipGraph.add("NumObjCount", GraphDesc.LINE,"Number of objects");
		
		ipGraph.setGraphName("squidnumobj");
		ipGraph.setGraphTitle("Number of Squid objects on {1}");
		ipGraph.setVerticalLabel("objetcs");
		ipGraph.setHostTree(new Object[] {GraphDesc.HOST, GraphDesc.SERVICES, "Squid", "Number of objects"} );
		ipGraph.setViewTree(new Object[] {GraphDesc.SERVICES, "Squid", GraphDesc.HOST, "Number of objects"});
		
		pd.setGraphClasses(new Object[] {ipGraph, SquidHitRatioGraph.class, SquidBytesGraph.class, SquidCpu.class, SquidReqGraph.class});
		pd.setName("squid");
	}
	
	/**
	 * @param monitoredHost
	 */
	public Squid(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
