/*
 * Created on 7 janv. 2005
 *
 * TODO 
 */
package jrds.probe.munins;

import org.rrd4j.DsType;

import jrds.ProbeDesc;
import jrds.RdsHost;

/**
 * @author bacchell
 *
 * TODO 
 */
public class IfSolarisMunins extends MuninsIndexedNameProbe {
	static final private ProbeDesc pd = new ProbeDesc(5);
	static {
		pd.add("ifInOctets", DsType.COUNTER, "rbytes.value");
		pd.add("ifOutOctets", DsType.COUNTER, "obytes.value");
		pd.add("ifInErrors", DsType.COUNTER, "ierrors.value");
		pd.add("ifOutErrors", DsType.COUNTER, "oerrors.value");
		pd.add("collisions", DsType.COUNTER, "collisions.value");
		pd.setGraphClasses(new Object[] {"ifbps"});
		pd.setMuninsProbesNames(new String[] { "if", "if_errcoll"});
		pd.setProbeName("if-{1}_munins");
	}

	/**
	 * @param monitoredHost
	 */
	public IfSolarisMunins(RdsHost monitoredHost, String indexName) {
		super(monitoredHost, pd, indexName);
	}
}
