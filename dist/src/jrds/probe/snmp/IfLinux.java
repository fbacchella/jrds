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
import jrds.graphe.IfPacketsGraph;

import org.snmp4j.smi.OID;


/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class IfLinux extends RdsIndexedSnmpRrd {
	static final private OID indexOid = new OID(".1.3.6.1.2.1.2.2.1.2");

	static final private ProbeDesc pd = new ProbeDesc(8);
	static {
		//pd.add("Speed", new OID(".1.3.6.1.2.1.2.2.1.5"));
		pd.add("ifInOctets", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.2.2.1.10"));
		pd.add("ifInUcastPkts", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.2.2.1.11"));
		pd.add("ifInDiscards", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.2.2.1.13"));
		pd.add("ifInErrors", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.2.2.1.14"));
		pd.add("ifOutOctets", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.2.2.1.16"));
		pd.add("ifOutUcastPkts", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.2.2.1.17"));
		pd.add("ifOutErrors", ProbeDesc.COUNTER, new OID(".1.3.6.1.2.1.2.2.1.20"));
		pd.setGraphClasses(new Class[] {IfGraph.class, IfPacketsGraph.class, IfPacketSize.class});
	}
	
	public IfLinux(RdsHost monitoredHost, String indexName) {
		super(monitoredHost, pd, indexName);
		setRrdName("if-" + getIndexName());
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsArraySnmpRrd#initIndexOid()
	 */
	protected OID initIndexOid() {
		return indexOid;
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsIndexedSnmpRrd#initIsUniq()
	 */
	protected boolean initIsUniq() {
		return true;
	}
}
