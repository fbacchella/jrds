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
public class IfSolarisMunins extends MuninsIndexedNameProbe {
	
	static private final Logger logger = JrdsLogger.getLogger(IfLinuxMunins.class.getPackage().getName());
	
	static final private ProbeDesc pd = new ProbeDesc(5);
	static {
		pd.add("ifInOctets", ProbeDesc.COUNTER, "rbytes.value");
		pd.add("ifOutOctets", ProbeDesc.COUNTER, "obytes.value");
		pd.add("ifInErrors", ProbeDesc.COUNTER, "ierrors.value");
		pd.add("ifOutErrors", ProbeDesc.COUNTER, "oerrors.value");
		pd.add("collisions", ProbeDesc.COUNTER, "collisions.value");
		pd.setGraphClasses(new Class[] {IfGraph.class});
		pd.setMuninsProbesNames(new String[] { "if", "if_errcoll"});
	}

	/**
	 * @param monitoredHost
	 */
	public IfSolarisMunins(RdsHost monitoredHost, String indexName) {
		super(monitoredHost, pd, indexName);
		setRrdName("if-" + this.getIndexName() + "_munins");
	}
}
