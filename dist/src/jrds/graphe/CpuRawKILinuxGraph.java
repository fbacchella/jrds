/*
 * Created on 8 déc. 2004
 *
 * TODO 
 */
package jrds.graphe;

import jrds.GraphDesc;
import jrds.Probe;
import jrds.RdsGraph;

/**
 * @author bacchell
 *
 * TODO 
 */
public class CpuRawKILinuxGraph extends RdsGraph {
	
	static final GraphDesc ds = new GraphDesc(2);
	static {
		ds.add("ssCpuRawKernel", GraphDesc.LINE, "kernel CPU time");
		ds.add("ssCpuRawInterrupt", GraphDesc.LINE, "interruptlevel CPU time");
 		ds.setFilename("cpurawkilinux");
		ds.setGraphTitle("Consommation  CPU");
		ds.setHostTree(GraphDesc.HSLT);
		ds.setViewTree(GraphDesc.SLHT);
}

	/**
	 * @param theStore
	 */
	public CpuRawKILinuxGraph(Probe theStore) {
		super(theStore, ds);
	}
}
