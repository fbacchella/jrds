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
public class CpuTimeSunMibGraph extends RdsGraph {
	
	static final GraphDesc ds = new GraphDesc(9);
	static {
		ds.add("rsUserProcessTime");
		ds.add("rsNiceModeTime");
		ds.add("rsSystemProcessTime");
		ds.add("rsIdleModeTime");
		ds.add("total", "rsUserProcessTime, rsUserProcessTime, +, rsSystemProcessTime, +, rsIdleModeTime, +");
		ds.add("user","rsUserProcessTime, total, /, 100, *", GraphDesc.AREA, Color.BLUE, "User");
		ds.add("nice","rsNiceModeTime, total, /, 100, *", GraphDesc.STACK, Color.CYAN, "Nice" );
		ds.add("system","rsSystemProcessTime, total, /, 100, *", GraphDesc.STACK, Color.RED, "System");
		ds.add("idle","rsIdleModeTime, total, /, 100, *", GraphDesc.STACK, Color.GREEN, "Idle");
		ds.setFilename("Temps CPU");
		ds.setGraphTitle("Utilisation CPU vue par la MIB SUN");
		ds.setUpperLimit(100);
		ds.setVerticalLabel("%");
		ds.setHostTree(GraphDesc.HSLT);
		ds.setViewTree(GraphDesc.SLHT);
	}

	/**
	 * @param theStore
	 */
	public CpuTimeSunMibGraph(Probe theStore) {
		super(theStore, ds);
	}
}
