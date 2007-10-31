/*
 * Created on 8 déc. 2004
 *
 * TODO 
 */
package jrds.graphe;

import java.awt.Color;

import jrds.GraphDesc;
import jrds.Probe;
import jrds.GraphNode;


/**
 * @author bacchell
 *
 * TODO 
 */
public class CpuTimeGraph extends GraphNode {
	static final GraphDesc gd = new GraphDesc(1);
	static {
		gd.add("ssCpuUser", GraphDesc.AREA, Color.BLUE, "User");
		gd.add("ssCpuSystem", GraphDesc.STACK, Color.RED, "System");
		gd.add("ssCpuIdle", GraphDesc.STACK, Color.GREEN, "Idle");
		gd.setGraphName("cputime");
		gd.setGraphTitle("Utilisation CPU on {1}");
		gd.setUpperLimit(100);
		gd.setVerticalLabel("%");
		gd.setHostTree(new Object[] {
				GraphDesc.HOST, GraphDesc.SYSTEM, GraphDesc.LOAD, "CPU usage"});
		gd.setViewTree(new Object[] {
				GraphDesc.SYSTEM, GraphDesc.LOAD, "CPU usage", GraphDesc.HOST});
	}

	/**
	 * @param theStore
	 */
	public CpuTimeGraph(Probe theStore) {
		super(theStore, gd);
	}
}
