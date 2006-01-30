package jrds.probe.snmp;


import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.snmp.SnmpRequester;

import org.snmp4j.smi.OID;


public class CiscoMem extends RdsSnmpSimple {
	static final private ProbeDesc pd = new ProbeDesc(2);
	static {
		pd.add("Installed Memory", ProbeDesc.GAUGE, new OID(".1.3.6.1.4.1.9.3.6.6"));
		pd.add("Free Memory", ProbeDesc.GAUGE, new OID(".1.3.6.1.4.1.9.2.1.8"));
		pd.setName("ciscomemory");
		pd.setRequester(SnmpRequester.SIMPLE);
		
		pd.setGraphClasses(new Object[] {"cpuload.xml"});
	}
	
	public CiscoMem(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
	
}
