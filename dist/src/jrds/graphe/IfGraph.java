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
import jrds.probe.IndexedProbe;


/**
 * @author bacchell
 *
 * TODO 
 */
public class IfGraph extends RdsGraph {
	static final GraphDesc ds = new GraphDesc(4);
	static {
		ds.add("ifOutOctets");
		ds.add("ifOutBits","ifOutOctets,8,*", GraphDesc.AREA, Color.GREEN,"bits sends/s");

		ds.add("ifInOctets");
		ds.add("ifInBitsInversed","ifInOctets,-8,*", GraphDesc.AREA, Color.BLUE,"bits received/s") ;

		ds.setLowerLimit(Double.NaN);
		ds.setVerticalLabel("bits/s");
		ds.setHostTree(GraphDesc.HNIIT);
		ds.setViewTree(GraphDesc.NIHIT);
	}
 	/**
	 * @param theStore
	 */
	public IfGraph(Probe theStore) {
		super(theStore, (GraphDesc) ds.clone());
		setFilename("if-" + ((IndexedProbe)probe).getIndexName());
		setGraphTitle("Interface " + ((IndexedProbe)probe).getIndexName());
	}
}
