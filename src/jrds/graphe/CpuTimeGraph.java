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
public class CpuTimeGraph extends RdsGraph {
	static final GraphDesc gd = new GraphDesc(1);
	static {
		gd.add("ssCpuUser", GraphDesc.AREA, Color.BLUE, "User");
		gd.add("ssCpuSystem", GraphDesc.STACK, Color.RED, "System");
		gd.add("ssCpuIdle", GraphDesc.STACK, Color.GREEN, "Idle");
		gd.setFilename("cputime");
		gd.setGraphName("Utilisation CPU");
		gd.setUpperLimit(100);
		gd.setVerticalLabel("%");
		gd.setHostTree(GraphDesc.HSLT);
		gd.setViewTree(GraphDesc.SLHT);
	}

	/**
	 * @param theStore
	 */
	public CpuTimeGraph(Probe theStore) {
		super(theStore, gd);
	}
}
