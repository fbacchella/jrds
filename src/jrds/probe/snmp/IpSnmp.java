/*
 * Created on 2 déc. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.probe.snmp;

import jrds.GraphDesc;
import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.RdsSnmpSimple;
import jrds.graphe.IpErrorGraph;
import jrds.snmp.SnmpRequester;

import org.snmp4j.smi.OID;


/**
 * @author bacchell
 *
 */
public class IpSnmp extends RdsSnmpSimple {
	static final private ProbeDesc pd = new ProbeDesc(8);
	static {
		pd.add("ipInReceives", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.4.3"));
		pd.add("ipInHdrErrors", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.4.4"));
		pd.add("ipInAddrErrors", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.4.5"));
		pd.add("ipForwDatagrams", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.4.6"));
		pd.add("ipInUnknownProtos", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.4.7"));
		pd.add("ipInDiscards", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.4.8"));
		pd.add("ipInDelivers", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.4.9"));
		pd.add("ipOutRequests", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.4.10"));
		pd.add("ipOutDiscards", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.4.11"));
		pd.add("ipOutNoRoutes", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.4.12"));
		pd.add("ipReasmReqds", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.4.14"));
		pd.add("ipReasmOKs", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.4.15"));
		pd.add("ipReasmFails", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.4.16"));
		pd.add("ipFragOKs", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.4.15"));
		pd.add("ipFragFails", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.4.18"));
		pd.add("ipFragCreates", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.4.19"));
		pd.add("ipRoutingDiscards", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.4.23"));


		pd.setRrdName("ipsnmp");
        pd.setRequester(SnmpRequester.SIMPLE);

		//The associated graph
		/*GraphDesc ipGraph = new GraphDesc(13);
		ipGraph.add("ipInReceives", GraphDesc.LINE);
		ipGraph.add("ipForwDatagrams", GraphDesc.LINE);
		ipGraph.add("ipInDelivers", GraphDesc.LINE);
		ipGraph.add("ipOutRequests", GraphDesc.LINE);
		ipGraph.add("ipReasmReqds", GraphDesc.LINE);
		ipGraph.add("ipReasmOKs", GraphDesc.LINE);
		ipGraph.add("ipFragOKs", GraphDesc.LINE);
		ipGraph.add("ipFragCreates", GraphDesc.LINE);

		ipGraph.setFilename("ip");
		ipGraph.setGraphTitle("IP activity");
		ipGraph.setVerticalLabel("paquets/s");
		ipGraph.setHostTree(new Object[] { GraphDesc.HOST, GraphDesc.NETWORK, GraphDesc.IP, GraphDesc.TITLE});
		ipGraph.setViewTree(new Object[] { GraphDesc.NETWORK, GraphDesc.IP, GraphDesc.HOST, GraphDesc.TITLE});*/

		pd.setGraphClasses(new Object[] { "ipactivitysnmp.xml", IpErrorGraph.class});
	}

	/**
	 * @param monitoredHost
	 */
	public IpSnmp(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
