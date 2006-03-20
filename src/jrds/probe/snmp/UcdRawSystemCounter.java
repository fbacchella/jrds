/*##########################################################################
 _##
 _##  $Id: CpuRawTimeLinux.java 186 2006-01-18 18:06:48 +0100 (mer., 18 janv. 2006) fbacchella $
 _##
 _##########################################################################*/

package jrds.probe.snmp;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.snmp.SnmpRequester;

import org.snmp4j.smi.OID;


/**
 * @author Fabrice Bacchella 
 * @version $Revision: 186 $,  $Date: 2006-01-18 18:06:48 +0100 (mer., 18 janv. 2006) $
 */
public class UcdRawSystemCounter extends RdsSnmpSimple {
	static final private ProbeDesc pd = new ProbeDesc(6);
	static {
		pd.add("ssIORawSent", ProbeDesc.COUNTER, new OID(".1.3.6.1.4.1.2021.11.57"));
		pd.add("ssIORawReceived", ProbeDesc.COUNTER, new OID(".1.3.6.1.4.1.2021.11.58"));
		pd.add("ssRawInterrupts", ProbeDesc.COUNTER, new OID(".1.3.6.1.4.1.2021.11.59"));
		pd.add("ssRawContexts", ProbeDesc.COUNTER, new OID(".1.3.6.1.4.1.2021.11.60"));
		pd.add("ssRawSwapIn", ProbeDesc.COUNTER, new OID(".1.3.6.1.4.1.2021.11.62"));
		pd.add("ssRawSwapOut", ProbeDesc.COUNTER, new OID(".1.3.6.1.4.1.2021.11.63"));
		pd.setProbeName("ucdrawsystem");
		pd.setGraphClasses(new Object[] { "ucdswap.xml"});
		pd.setRequester(SnmpRequester.SIMPLE);
	}
	
	/**
	 * @param monitoredHost
	 */
	public UcdRawSystemCounter(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
