/*
 * Created on 29 déc. 2004
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
public class MemGraph extends RdsGraph {
	static final GraphDesc ds = new GraphDesc(6);
	static {
		ds.add("memTotalSwap");
		ds.add("memAvailSwap");
		ds.add("memTotalReal");
		ds.add("memAvailReal");

		ds.add("memUsedRealBytes", "memTotalReal,memAvailReal,-, 1024, *", GraphDesc.AREA, Color.GREEN, "Active Real/Physical Memory Space");
		ds.add("memAvailRealBytes", "memAvailReal, 1024, *", GraphDesc.STACK, Color.BLUE, "Available Real/Physical Memory Space");
		ds.add("memUsedSwapBytes", "memTotalSwap,memAvailSwap,-, 1024, *", GraphDesc.STACK, Color.RED, "Used Swap Space");
		ds.setGraphName("memory");
		ds.setGraphTitle("Memory usage on {1}");
	
		ds.setHostTree(GraphDesc.HSMT);
		ds.setViewTree(GraphDesc.SMHT);
	}

	/**
	 * @param theStore
	 */
	public MemGraph(Probe theStore) {
		super(theStore, ds);
	}

}
