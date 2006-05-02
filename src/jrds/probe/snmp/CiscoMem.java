package jrds.probe.snmp;


import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.snmp.SnmpRequester;

import org.rrd4j.DsType;
import org.snmp4j.smi.OID;


public class CiscoMem extends RdsSnmpSimple {
	static final private ProbeDesc pd = new ProbeDesc(2);
	static {
		pd.add("Installed Memory", DsType.GAUGE, new OID(".1.3.6.1.4.1.9.3.6.6"));
		pd.add("Free Memory", DsType.GAUGE, new OID(".1.3.6.1.4.1.9.2.1.8"));
		pd.setProbeName("ciscomemory");
		pd.setRequester(SnmpRequester.SIMPLE);
		
		pd.setGraphClasses(new Object[] {"cpuload"});
	}
	
	public CiscoMem(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
	
}
