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
public class CpuLoadGraph extends RdsGraph {
	static final GraphDesc gd = new GraphDesc(3);
	static {
		gd.setFilename("LoadAverage");
		gd.setGraphTitle("Charge CPU");
		gd.add("la1",GraphDesc.LINE, Color.GREEN,"1mn");
		gd.add("la5",GraphDesc.LINE, Color.BLUE,"5mn");
		gd.add("la15",GraphDesc.LINE, Color.RED,"15mn");
		gd.setVerticalLabel("queue size");
		gd.setHostTree(GraphDesc.HSLT);
		gd.setViewTree(GraphDesc.SLHT);
	}
	/**
	 * @param theStore
	 */
	public CpuLoadGraph(Probe theStore) {
		super(theStore, gd);
	}
}
