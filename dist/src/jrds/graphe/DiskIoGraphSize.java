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
public class DiskIoGraphSize extends RdsGraph {
	static final GraphDesc gd = new GraphDesc(6);
	static {
		gd.add("diskIONRead");
		gd.add("diskIONWritten");
		gd.add("diskIOReads");
		gd.add("diskIOWrites");
		gd.add("diskIOBLKRSZ", "diskIONRead, diskIOReads, /", GraphDesc.LINE,Color.GREEN,"Average request size on read");
		gd.add("diskIOBLKWSZ", "diskIONWritten, diskIOWrites, /", GraphDesc.LINE,Color.BLUE,"Average request size on write");
		gd.setVerticalLabel("octets");
		gd.setHostTree(GraphDesc.HDAIT);
		gd.setViewTree(GraphDesc.DAHIT);
		gd.setSubTitle("Average request size");
	}

	/**
	 * @param theStore
	 */
	public DiskIoGraphSize(Probe theStore) {
		super(theStore, gd);
		setFilename("bsz." + probe.getName());
		setGraphTitle("E/S disque "+ ((IndexedProbe)probe).getIndexName() + "(taille des requetes)");
	}
}
