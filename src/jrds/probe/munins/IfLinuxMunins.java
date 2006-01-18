/*
 * Created on 7 janv. 2005
 *
 * TODO 
 */
package jrds.probe.munins;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.IfGraph;

import org.apache.log4j.Logger;


/**
 * @author bacchell
 *
 * TODO 
 */
public class IfLinuxMunins extends MuninsIndexedNameProbe {
	
	static private final Logger logger = Logger.getLogger(IfLinuxMunins.class);
	
	static final private ProbeDesc pd = new ProbeDesc(4);
	static {
		pd.add("ifInOctets", ProbeDesc.COUNTER, "down.value");
		pd.add("ifOutOctets", ProbeDesc.COUNTER, "up.value");
		pd.add("ifInErrors", ProbeDesc.COUNTER, "rcvd.value");
		pd.add("ifOutErrors", ProbeDesc.COUNTER, "trans.value");
		pd.setMuninsProbesNames(new String[] { "if", "if_err"});
		pd.setGraphClasses(new Class[] {IfGraph.class});
	}

	/**
	 * @param monitoredHost
	 */
	public IfLinuxMunins(RdsHost monitoredHost, String indexName) {
		super(monitoredHost, pd, indexName);
		setName("if-" + getIndexName() + "_munins");
	}
}
