/*
 * Created on 8 d�c. 2004
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
public class DiskIoGraphReq extends RdsGraph {
	private static final GraphDesc gd = new GraphDesc(2);
	static {
		gd.add("diskIOReads", GraphDesc.LINE, Color.GREEN,"Number of read accesses");
		gd.add("diskIOWrites", GraphDesc.LINE, Color.BLUE,"Number of write accesses");
		gd.setVerticalLabel("operations/s");
		gd.setHostTree(GraphDesc.HDAIT);
		gd.setViewTree(GraphDesc.DAHIT);
		gd.setSubTitle("Activity as operation/s");
	}

	/**
	 * @param theStore
	 */
	public DiskIoGraphReq(Probe theStore) {
		super(theStore, gd);
		setFilename("op." + probe.getName());
		setGraphTitle("E/S disque " + ((IndexedProbe)probe).getIndexName() +"(operations)");
	}
}
