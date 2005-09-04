/*
 * Created on 3 févr. 2005
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
public class VMSolarisGraph extends RdsGraph {
	
	static final GraphDesc gd = new GraphDesc(4);
	static {
		gd.add("rsVPagesIn", GraphDesc.LINE, "Number of pages read in from disk");
		gd.add("rsVPagesOut", GraphDesc.LINE, "Number of pages written to disk");
		gd.add("rsVSwapIn", GraphDesc.LINE, "Number of pages swapped in");
		gd.add("rsVSwapOut", GraphDesc.LINE, "Number of pages swapped out");
		
		gd.setGraphName("vmsolaris");
		gd.setGraphName("VM activity");
		gd.setVerticalLabel("Operation/s");
		gd.setHostTree(new Object[] { GraphDesc.HOST, GraphDesc.SYSTEM, GraphDesc.MEMORY, GraphDesc.TITLE});
		gd.setViewTree(new Object[] { GraphDesc.SYSTEM,  GraphDesc.MEMORY, GraphDesc.TITLE, GraphDesc.HOST});
	}
	
	/**
	 * @param theStore
	 */
	public VMSolarisGraph(Probe theStore) {
		super(theStore, gd);
	}
}
