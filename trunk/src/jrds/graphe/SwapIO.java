/*
 * Created on 7 janv. 2005
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
public class SwapIO extends GraphNode {
	
	private static final GraphDesc gd = new GraphDesc(3);
	static {
		gd.setGraphName("swapio");
		gd.setHostTree(new Object[] {GraphDesc.HOST,GraphDesc.SYSTEM,  GraphDesc.MEMORY, GraphDesc.TITLE});
		gd.setViewTree(new Object[] {GraphDesc.SYSTEM, GraphDesc.MEMORY, GraphDesc.TITLE, GraphDesc.HOST});
		gd.add("swapIn", GraphDesc.AREA, Color.BLUE, "Swap in");
		gd.add("swapOut");
		gd.add("swapOutInverser","0,swapOut,-", GraphDesc.AREA, Color.GREEN, "Swap out");
		gd.setGraphName("Swap activity");
	}

	/**
	 * @param theStore
	 */
	public SwapIO(Probe theStore) {
		super(theStore, gd);
	}
}
