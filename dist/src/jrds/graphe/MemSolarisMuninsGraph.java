/*
 * Created on 29 d�c. 2004
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
public class MemSolarisMuninsGraph extends RdsGraph {
	static final GraphDesc gd = new GraphDesc(1);
	static {
		gd.add("memTotalRealMB");
		gd.add("memUsedRealMB");
		gd.add("memTotalSwapMB");
		gd.add("memUsedSwapMB");
		gd.add("memAvailRealMB","memTotalRealMB, memUsedRealMB,-");
		gd.add("memAvailSwapMB","memTotalSwapMB, memUsedSwapMB, -");
		
		gd.add("memUsedReal", "memUsedRealMB, 1024, *, 1024, *", GraphDesc.AREA, "Used Real/Physical Memory Space");
		gd.add("memAvailReal", "memAvailRealMB, 1024, *, 1024, *",GraphDesc.STACK, "Available Real/Physical Memory Space");
		gd.add("memUsedSwap", "memUsedSwapMB, 1024, *, 1024, *", GraphDesc.STACK,Color.RED,"Used Swap Space");
		gd.setFilename("memsolarismunins");
		gd.setGraphTitle("Utilisation m�moire");
		gd.setVerticalLabel("Bytes");
		gd.setHostTree(GraphDesc.HSMT);
		gd.setViewTree(GraphDesc.SMHT);
	}

	/**
	 * @param theStore
	 */
	public MemSolarisMuninsGraph(Probe theStore) {
		super(theStore, gd);
	}
}
