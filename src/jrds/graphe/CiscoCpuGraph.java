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
public class CiscoCpuGraph extends RdsGraph {
	static final GraphDesc gd = new GraphDesc(2);
	static {
		gd.add("la1", GraphDesc.LINE, Color.GREEN, "1mn");
		gd.add("la5", GraphDesc.LINE, Color.BLUE, "5mn");
		gd.setFilename("ciscocpu");
		gd.setGraphTitle("Charge CPU");
		gd.setHostTree(GraphDesc.HSLT);
		gd.setViewTree(GraphDesc.SLHT);
	}

	/**
	 * @param theStore
	 */
	public CiscoCpuGraph(Probe theStore) {
		super(theStore, gd);
	}
}
