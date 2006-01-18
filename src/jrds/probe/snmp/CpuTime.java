/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds.probe.snmp;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.CpuTimeGraph;
import jrds.snmp.SnmpRequester;

import org.snmp4j.smi.OID;


/**
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class CpuTime extends RdsSnmpSimple {
	
	static final ProbeDesc pd = new ProbeDesc(5);
	static {
		pd.add("ssCpuUser", ProbeDesc.GAUGE, new OID(".1.3.6.1.4.1.2021.11.9"));
		pd.add("ssCpuSystem", ProbeDesc.GAUGE, new OID(".1.3.6.1.4.1.2021.11.10"));
		pd.add("ssCpuIdle", ProbeDesc.GAUGE, new OID(".1.3.6.1.4.1.2021.11.11"));
		pd.add("NumProcess", ProbeDesc.GAUGE, new OID(".1.3.6.1.2.1.25.1.6."));
		pd.add("NumUsers", ProbeDesc.GAUGE, new OID(".1.3.6.1.2.1.25.1.5"));
		pd.setName("cpu");
		pd.setGraphClasses(new Class[] {CpuTimeGraph.class});
		pd.setRequester(SnmpRequester.SIMPLE);
		//pd.setGraphClasses(new Object[] {"cputime.xml"});
	}
	/**
	 * @param monitoredHost
	 */
	public CpuTime(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
	
}
