/*
 * Created on 7 janv. 2005
 *
 * TODO 
 */
package jrds.probe.munins;

import jrds.JrdsLogger;
import jrds.MuninsIndexedNameProbe;
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
	
	static private final Logger logger = JrdsLogger.getLogger(IfLinuxMunins.class.getPackage().getName());
	
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
		setRrdName("if-" + getIndexName() + "_munins");
	}
}
