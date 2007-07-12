package jrds.probe.snmp;


import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.snmp.SnmpRequester;

import org.rrd4j.DsType;
import org.snmp4j.smi.OID;


public class CiscoTemp extends RdsSnmpSimple {
	static final private ProbeDesc pd = new ProbeDesc(2);
	static {
		pd.add("inlet temperature", DsType.GAUGE, new OID(".1.3.6.1.4.1.9.9.13.1.3.1.3.1"));
		pd.add("outlet temperature", DsType.GAUGE, new OID(".1.3.6.1.4.1.9.9.13.1.3.1.3.2"));
		pd.setProbeName("ciscotemperature");
		pd.setRequester(SnmpRequester.SIMPLE);
		
		pd.setGraphClasses(new Object[] {"cpuload"});
	}
	
	public CiscoTemp(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
	
}
