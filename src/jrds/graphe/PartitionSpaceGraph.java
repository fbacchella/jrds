/*
 * Created on 27 déc. 2004
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
public class PartitionSpaceGraph extends RdsGraph {
	static final private GraphDesc gd= new GraphDesc(2);
	static {
		gd.add("Total");
		gd.add("Used", GraphDesc.AREA, Color.BLUE, "Used Space");
		gd.add("Available", "Total,Used,-", GraphDesc.STACK, Color.GREEN, "Available space");
		gd.setHostTree(new Object[] {GraphDesc.HOST, GraphDesc.DISK, "Systèmes de fichiers", GraphDesc.INDEX});
		gd.setViewTree(new Object[] {GraphDesc.DISK, "Systèmes de fichiers", GraphDesc.HOST, GraphDesc.INDEX});
		gd.setGraphTitle("Occupation of file system {2} on {1}");
		gd.setGraphName("{4}");
		
	}
	/**
	 * @param theStore
	 */
	public PartitionSpaceGraph(Probe theStore) {
		super(theStore, gd);
	}
}
