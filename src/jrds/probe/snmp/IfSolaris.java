/*
 * Created on 30 nov. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.probe.snmp;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.RdsIndexedSnmpRrd;
import jrds.graphe.IfGraph;
import jrds.graphe.IfPacketSize;

import org.snmp4j.smi.OID;


/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class IfSolaris extends RdsIndexedSnmpRrd {
	static final private ProbeDesc pd = new ProbeDesc(12);
	static {
		pd.add("Speed", ProbeDesc.COUNTER, new OID("1.3.6.1.2.1.2.2.1.5"));
		pd.add("ifInOctets", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.2.2.1.10"));
		pd.add("ifInUcastPkts", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.2.2.1.11"));
		pd.add("ifInNUcastPkts", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.2.2.1.12"));
		pd.add("ifInDiscards", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.2.2.1.13"));
		pd.add("ifInErrors", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.2.2.1.14"));
		pd.add("ifOutOctets", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.2.2.1.16"));
		pd.add("ifOutUcastPkts", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.2.2.1.17"));
		pd.add("ifOutNUcastPkts", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.2.2.1.18"));
		pd.add("ifOutDiscards", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.2.2.1.19"));
		pd.add("ifOutErrors", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.2.2.1.20"));
		pd.setGraphClasses(new Object[] {IfGraph.class, "ifpacketssnmp.xml", IfPacketSize.class});
		pd.setIndexOid(new OID(".1.3.6.1.2.1.2.2.1.2"));
	}

	public IfSolaris(RdsHost monitoredHost, String indexName) {
		super(monitoredHost, pd, indexName);
		setRrdName("if-" + getIndexName());
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsIndexedSnmpRrd#initIsUniq()
	 */
	protected boolean initIsUniq() {
		return true;
	}

}
