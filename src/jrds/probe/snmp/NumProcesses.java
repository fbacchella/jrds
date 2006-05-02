/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds.probe.snmp;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.snmp.SnmpRequester;

import org.rrd4j.DsType;
import org.snmp4j.smi.OID;


/**
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class NumProcesses extends RdsSnmpSimple {
	static final private OID hrSystemProcesses = new OID(".1.3.6.1.2.1.25.1.6");

	static final private ProbeDesc pd = new ProbeDesc(1);
	static {
		pd.add("hrSystemProcesses", DsType.GAUGE, hrSystemProcesses);
		pd.setGraphClasses(new Object []{"numprocess"});
		pd.setProbeName("nprocesses");
		pd.setRequester(SnmpRequester.SIMPLE);
	}
	
	/**
	 * @param monitoredHost
	 */
	public NumProcesses(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}

}
