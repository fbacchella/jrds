/*
 * Created on 16 déc. 2004
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
public class SybaseGraph extends RdsGraph {
	static final private GraphDesc gd = new GraphDesc(5);
	static {
		gd.add("data", GraphDesc.AREA, Color.BLUE, "data");
		gd.add("index_size", GraphDesc.STACK, Color.GREEN, "index");
		gd.add("unused", GraphDesc.AREA, new Color(0,255,255), "unused");
		gd.add("database_size", GraphDesc.LINE, Color.RED, "total size");
		gd.add("reserved", GraphDesc.LINE, Color.BLACK, "reserved");
		gd.setVerticalLabel("bytes");
		gd.setHostTree(GraphDesc.HSDJT);
		gd.setViewTree(GraphDesc.SDJT);
	}

	/**
	 * @param theStore
	 */
	public SybaseGraph(Probe theStore) {
		super(theStore, gd);
		setGraphName(theStore.getName());
		setGraphTitle("Occupation disque de la base " + ((jrds.probe.Sybase) probe).getDbName());
	}
}
