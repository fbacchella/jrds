/*
 * Created on 7 janv. 2005
 *
 * TODO 
 */
package jrds.probe.munins;

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
		pd.add("ifInOctets", ProbeDesc.COUNTER, "rbytes.value");
		pd.add("ifOutOctets", ProbeDesc.COUNTER, "obytes.value");
		pd.add("ifInErrors", ProbeDesc.COUNTER, "ierrors.value");
		pd.add("ifOutErrors", ProbeDesc.COUNTER, "oerrors.value");
		pd.add("collisions", ProbeDesc.COUNTER, "collisions.value");
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
