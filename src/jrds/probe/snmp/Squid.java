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

import org.rrd4j.DsType;
import org.snmp4j.smi.OID;


/**
 * @author bacchell
 *
 * TODO 
 */
public class Squid extends RdsSnmpSimple {
	static final private ProbeDesc pd = new ProbeDesc(44);
	static {
		pd.add("SysPageFaults", DsType.COUNTER, new OID(".1.3.6.1.4.1.3495.1.3.1.1"));
		pd.add("SysNumReads", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.1.2"));
		pd.add("DnsNumberServers",DsType.GAUGE,new OID(".1.3.6.1.4.1.3495.1.4.3.3"));
		pd.add("DnsReplies", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.4.3.2"), 0, 10000);
		pd.add("DnsRequests", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.4.3.1"), 0, 10000);
		pd.add("BlkGetHostByAddr", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.4.2.7"));
		pd.add("FqdnMisses", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.4.2.6"));
		pd.add("FqdnNegativeHits", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.4.2.5"));
		pd.add("FqdnPendingHits", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.4.2.4"));
		pd.add("FqdnHits", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.4.2.3"), 0, 10000);
		pd.add("FqdnRequests", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.4.2.2"), 0, 10000);
		pd.add("FqdnEntries",DsType.GAUGE,new OID(".1.3.6.1.4.1.3495.1.4.2.1"), 0, 10000);
		pd.add("AttemptReleaseLckEnt", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.4.1.8"));
		pd.add("BlkGetHostByName", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.4.1.7"));
		pd.add("IpMisses",DsType.GAUGE,new OID(".1.3.6.1.4.1.3495.1.4.1.6"), 0, 10000);
		pd.add("IpNegativeHits",DsType.GAUGE,new OID(".1.3.6.1.4.1.3495.1.4.1.5"), 0, 10000);
		pd.add("IpPendingHits", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.4.1.4"), 0, 10000);
		pd.add("IpHits", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.4.1.3"), 0, 10000);
		pd.add("IpRequests", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.4.1.2"), 0, 10000);
		pd.add("IpEntries",DsType.GAUGE,new OID(".1.3.6.1.4.1.3495.1.4.1.1"));
		pd.add("Clients", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.15"));
		pd.add("CurrentSwapSize",DsType.GAUGE,new OID(".1.3.6.1.4.1.3495.1.3.2.1.14"));
		pd.add("ServerOutKb", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.13"), 0, 100000);
		pd.add("ServerInKb", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.12"), 0, 100000);
		pd.add("ServerErrors", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.11"), 0, 10000);
		pd.add("ServerRequests", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.10"), 0, 10000);
		pd.add("IcpKbRecv", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.9"), 0, 100000);
		pd.add("IcpKbSent", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.8"), 0, 100000);
		pd.add("IcpPktsRecv", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.7"));
		pd.add("IcpPktsSent", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.6"));
		pd.add("HttpOutKb", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.5"), 0, 100000);
		pd.add("HttpInKb", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.4"), 0, 100000);
		pd.add("HttpErrors", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.3"), 0, 10000);
		pd.add("HttpHits", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.2"), 0, 10000);
		pd.add("HttpRqt", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.2.1.1"), 0, 10000);
		pd.add("CurrentResFileDC",DsType.GAUGE,new OID(".1.3.6.1.4.1.3495.1.3.1.11"));
		pd.add("CurrentUnusedFDC",DsType.GAUGE,new OID(".1.3.6.1.4.1.3495.1.3.1.10"));
		pd.add("CurrentUnlinkRe",DsType.GAUGE,new OID(".1.3.6.1.4.1.3495.1.3.1.9"));
		pd.add("CurrentLRUExp", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.1.8"));
		pd.add("NumObjCount",DsType.GAUGE,new OID(".1.3.6.1.4.1.3495.1.3.1.7"));
		pd.add("MaxResSize",DsType.GAUGE,new OID(".1.3.6.1.4.1.3495.1.3.1.6"));
		pd.add("CpuUsage",DsType.GAUGE,new OID(".1.3.6.1.4.1.3495.1.3.1.5"));
		pd.add("CpuTime", DsType.COUNTER,new OID(".1.3.6.1.4.1.3495.1.3.1.4"));
		pd.add("MemUsage",DsType.GAUGE,new OID(".1.3.6.1.4.1.3495.1.3.1.3"));
		
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
		pd.setProbeName("squid");
	}
	
	/**
	 * @param monitoredHost
	 */
	public Squid(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
	public Squid() {
		super();
		this.setPd(pd);
	}
}
