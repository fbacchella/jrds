/*
 * Created on 29 déc. 2004
 *
 * TODO 
 */
package jrds.probe.snmp;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.MemGraphLinux;
import jrds.snmp.SnmpRequester;

import org.rrd4j.DsType;
import org.snmp4j.smi.OID;


/**
 * @author bacchell
 *
 * TODO 
 */
public final class MemLinux extends RdsSnmpSimple {
	static final private ProbeDesc pd = new ProbeDesc(7);
	static {
		pd.add("memTotalSwap", DsType.GAUGE, new OID(".1.3.6.1.4.1.2021.4.3"));
		pd.add("memAvailSwap", DsType.GAUGE, new OID(".1.3.6.1.4.1.2021.4.4"));
		pd.add("memTotalReal", DsType.GAUGE, new OID(".1.3.6.1.4.1.2021.4.5"));
		pd.add("memAvailReal", DsType.GAUGE, new OID(".1.3.6.1.4.1.2021.4.6"));
		pd.add("memShared", DsType.GAUGE, new OID(".1.3.6.1.4.1.2021.4.13"));
		pd.add("memBuffer", DsType.GAUGE, new OID(".1.3.6.1.4.1.2021.4.14"));
		pd.add("memCached", DsType.GAUGE, new OID(".1.3.6.1.4.1.2021.4.15"));
		pd.setGraphClasses(new Class[] {MemGraphLinux.class});
	       pd.setRequester(SnmpRequester.SIMPLE);
		pd.setProbeName("memlinux");
	}

	/**
	 * @param monitoredHost
	 */
	public MemLinux(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
