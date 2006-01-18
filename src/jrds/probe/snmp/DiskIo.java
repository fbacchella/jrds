/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds.probe.snmp;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.DiskIoGraphBytes;
import jrds.graphe.DiskIoGraphReq;
import jrds.graphe.DiskIoGraphSize;

import org.snmp4j.smi.OID;


/**
 * A class to probe the disk IO, using Net-SNMP
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class DiskIo extends RdsIndexedSnmpRrd {
	static final private ProbeDesc pd = new ProbeDesc(7);
	static {
		pd.add("diskIONRead", ProbeDesc.COUNTER, new OID(".1.3.6.1.4.1.2021.13.15.1.1.3")); 
		pd.add("diskIONWritten", ProbeDesc.COUNTER, new OID(".1.3.6.1.4.1.2021.13.15.1.1.4"));
		pd.add("diskIOReads", ProbeDesc.COUNTER, new OID(".1.3.6.1.4.1.2021.13.15.1.1.5"));
		pd.add("diskIOWrites", ProbeDesc.COUNTER, new OID(".1.3.6.1.4.1.2021.13.15.1.1.6"));
		pd.setGraphClasses(new Class[] {DiskIoGraphBytes.class, DiskIoGraphReq.class, DiskIoGraphSize.class});
		pd.setIndexOid(new OID(".1.3.6.1.4.1.2021.13.15.1.1.2"));
		pd.setName("io-{1}");
		pd.setUniqIndex(true);
	}

	/**
	 * @param monitoredHost
	 * @param indexName
	 */
	public DiskIo(RdsHost monitoredHost, String indexName) {
		super(monitoredHost, pd, indexName);
	}
}
