/*
 * Created on 7 janv. 2005
 *
 * TODO 
 */
package jrds.probe.munins;

import jrds.ProbeDesc;
import jrds.RdsHost;

import org.rrd4j.DsType;


/**
 * @author bacchell
 *
 * TODO 
 */
public class IfLinuxMunins extends MuninsIndexedNameProbe {
	
	static final private ProbeDesc pd = new ProbeDesc(4);
	static {
		pd.add("ifInOctets", DsType.COUNTER, "down.value");
		pd.add("ifOutOctets", DsType.COUNTER, "up.value");
		pd.add("ifInErrors", DsType.COUNTER, "rcvd.value");
		pd.add("ifOutErrors", DsType.COUNTER, "trans.value");
		pd.setMuninsProbesNames(new String[] { "if", "if_err"});
		pd.setGraphClasses(new Object[] {"ifbps"});
		pd.setProbeName("if-{1}_munins");
	}

	/**
	 * @param monitoredHost
	 */
	public IfLinuxMunins(RdsHost monitoredHost, String indexName) {
		super(monitoredHost, pd, indexName);
	}
}
