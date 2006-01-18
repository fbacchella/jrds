/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds.probe.snmp;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.IfGraph;
import jrds.graphe.IfPacketSize;

import org.snmp4j.smi.OID;


/**
 * A class to probe the NIC activity, using the MIB-II
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class IfLinux extends RdsIndexedSnmpRrd {
	static final private ProbeDesc pd = new ProbeDesc(8);
	static {
		pd.add("ifInOctets", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.2.2.1.10"));
		pd.add("ifInUcastPkts", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.2.2.1.11"));
		pd.add("ifInDiscards", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.2.2.1.13"));
		pd.add("ifInErrors", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.2.2.1.14"));
		pd.add("ifOutOctets", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.2.2.1.16"));
		pd.add("ifOutUcastPkts", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.2.2.1.17"));
		pd.add("ifOutErrors", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.2.2.1.20"));
		pd.setGraphClasses(new Object[] {IfGraph.class, "ifpacketssnmp.xml", IfPacketSize.class});
		pd.setIndexOid(new OID(".1.3.6.1.2.1.2.2.1.2"));
		pd.setName("if-{1}");
		pd.setUniqIndex(true);
	}
	
	public IfLinux(RdsHost monitoredHost, String indexName) {
		super(monitoredHost, pd, indexName);
	}
}
