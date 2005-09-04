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
public class DiskIoGraphReq extends RdsGraph {
	private static final GraphDesc gd = new GraphDesc(2);
	static {
		gd.add("diskIOReads", GraphDesc.LINE, Color.GREEN,"Number of read accesses");
		gd.add("diskIOWrites", GraphDesc.LINE, Color.BLUE,"Number of write accesses");
		gd.setVerticalLabel("operations/s");
		gd.setHostTree(new Object[] {
				GraphDesc.HOST, GraphDesc.DISK, GraphDesc.DISKACTIVITY, GraphDesc.INDEX, GraphDesc.TITLE});
		gd.setViewTree(new Object[] {
						GraphDesc.DISK, GraphDesc.DISKACTIVITY, GraphDesc.HOST, "Activity as operation/s", GraphDesc.TITLE});
	}

	/**
	 * @param theStore
	 */
	public DiskIoGraphReq(Probe theStore) {
		super(theStore, gd);
		setGraphName("op." + probe.getName());
		setGraphTitle("E/S disque " + ((IndexedProbe)probe).getIndexName() +"(operations)");
	}
}
