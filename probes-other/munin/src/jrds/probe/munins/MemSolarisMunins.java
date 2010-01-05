/*
 * Created on 11 janv. 2005
 *
 * TODO 
 */
package jrds.probe.munins;

import jrds.ProbeDesc;

import org.rrd4j.DsType;


/**
 * @author bacchell
 *
 * TODO 
 */
public class MemSolarisMunins extends Munin {
	static final private ProbeDesc pd = new ProbeDesc(4);
	static {
		
		pd.add("memTotalRealMB", DsType.GAUGE, "real.value");
		pd.add("memUsedRealMB", DsType.GAUGE, "used.value");
		pd.add("memTotalSwapMB", DsType.GAUGE, "swapt.value");
		pd.add("memUsedSwapMB", DsType.GAUGE, "swapu.value");
		pd.setProbeName("memsolarismunins");
//		pd.setMuninsProbesNames(new String[] { "memory"});
//		pd.setGraphClasses(new Class[] {MemSolarisMuninsGraph.class});
	}
}
