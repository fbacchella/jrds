/*
 * Created on 11 janv. 2005
 *
 * TODO 
 */
package jrds.probe.munins;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.MemSolarisMuninsGraph;

import org.apache.log4j.Logger;


/**
 * @author bacchell
 *
 * TODO 
 */
public class MemSolarisMunins extends MuninsProbe {
	static private final Logger logger = Logger.getLogger(MemSolarisMunins.class);

	static final private ProbeDesc pd = new ProbeDesc(4);
	static {
		
		pd.add("memTotalRealMB", ProbeDesc.GAUGE, "real.value");
		pd.add("memUsedRealMB", ProbeDesc.GAUGE, "used.value");
		pd.add("memTotalSwapMB", ProbeDesc.GAUGE, "swapt.value");
		pd.add("memUsedSwapMB", ProbeDesc.GAUGE, "swapu.value");
		pd.setName("memsolarismunins");
		pd.setMuninsProbesNames(new String[] { "memory"});
		pd.setGraphClasses(new Class[] {MemSolarisMuninsGraph.class});
	}
	
	/**
	 * @param monitoredHost
	 */
	public MemSolarisMunins(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
