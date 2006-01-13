/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds.probe.snmp;


import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.RdsSnmpSimple;
import jrds.snmp.SnmpRequester;

import org.snmp4j.smi.OID;


/**
 * A class to probe the CPU load on a cisco
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class CiscoCpu extends RdsSnmpSimple {
	static final private ProbeDesc pd = new ProbeDesc(2);
	static {
		pd.add("la1", ProbeDesc.GAUGE, new OID(".1.3.6.1.4.1.9.2.1.57"));
		pd.add("la5", ProbeDesc.GAUGE, new OID(".1.3.6.1.4.1.9.2.1.58"));
		pd.setRrdName("ciscocpuload");
		pd.setRequester(SnmpRequester.SIMPLE);
		
		pd.setGraphClasses(new Object[] {"cpuload.xml"});
	}
	
	/**
	 * A basic, default constructor
	 * @param monitoredHost
	 */
	public CiscoCpu(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
	
}
