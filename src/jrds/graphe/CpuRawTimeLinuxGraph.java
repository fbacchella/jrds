/*
 * Created on 8 déc. 2004
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
public class CpuRawTimeLinuxGraph extends RdsGraph {
	
	static final GraphDesc ds = new GraphDesc(11);
	static {
		ds.add("ssCpuRawUser");
		ds.add("ssCpuRawNice");
		ds.add("ssCpuRawSystem");
		ds.add("ssCpuRawWait");
		ds.add("ssCpuRawIdle");

		ds.add("total", "ssCpuRawUser, ssCpuRawNice, +, ssCpuRawSystem, +, ssCpuRawIdle, +, ssCpuRawWait, +");
		ds.add("Userpc","ssCpuRawUser, total, /, 100, *", GraphDesc.AREA, Color.BLUE, "User CPU time");
		ds.add("Nicepc","ssCpuRawNice, total, /, 100, *", GraphDesc.STACK, Color.ORANGE, "Nice CPU time" );
		ds.add("Waitpc","ssCpuRawWait, total, /, 100, *", GraphDesc.STACK, Color.CYAN, "Iowait CPU time" );
		ds.add("Systempc","ssCpuRawSystem, total, /, 100, *", GraphDesc.STACK, Color.RED, "System CPU time");
		ds.add("Idlepc","ssCpuRawIdle, total, /, 100, *", GraphDesc.STACK, Color.GREEN, "Idle CPU time");
		
		ds.setFilename("cpurawlinux");
		ds.setGraphName("Utilisation CPU");
		ds.setUpperLimit(100);
		ds.setVerticalLabel("%");
		ds.setHostTree(GraphDesc.HSLT);
		ds.setViewTree(GraphDesc.SLHT);
}

	/**
	 * @param theStore
	 */
	public CpuRawTimeLinuxGraph(Probe theStore) {
		super(theStore, ds);
	}
}
