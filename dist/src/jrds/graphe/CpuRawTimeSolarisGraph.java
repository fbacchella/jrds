/*
 * Created on 8 d�c. 2004
 *
 * TODO 
 */
package jrds.graphe;

import java.awt.Color;

import jrds.GraphDesc;
import jrds.Probe;
import jrds.RdsGraph;


/**
 * @author bacchell
 *
 * TODO 
 */
public class CpuRawTimeSolarisGraph extends RdsGraph {
	
	static final GraphDesc ds = new GraphDesc(9);
	static {
		ds.add("ssCpuRawUser");
		ds.add("ssCpuRawWait");
		ds.add("ssCpuRawIdle");
		ds.add("ssCpuRawKernel");
		ds.add("total", "ssCpuRawUser, ssCpuRawIdle, +, ssCpuRawWait, +, ssCpuRawKernel, +");
		ds.add("Userpc","ssCpuRawUser, total, /, 100, *", GraphDesc.AREA, Color.BLUE, "User");
		ds.add("Waitpc","ssCpuRawWait, total, /, 100, *", GraphDesc.STACK, Color.CYAN, "IO Wait" );
		ds.add("Kernelpc","ssCpuRawKernel, total, /, 100, *", GraphDesc.STACK, Color.RED, "Kernel");
		ds.add("Idlepc","ssCpuRawIdle, total, /, 100, *", GraphDesc.STACK, Color.GREEN, "Idle");
		ds.setFilename("Temps CPU");
		ds.setGraphTitle("Utilisation CPU");
		ds.setUpperLimit(100);
		ds.setVerticalLabel("%");
		ds.setHostTree(GraphDesc.HSLT);
		ds.setViewTree(GraphDesc.SLHT);
	}

	/**
	 * @param theStore
	 */
	public CpuRawTimeSolarisGraph(Probe theStore) {
		super(theStore, ds);
	}
}
