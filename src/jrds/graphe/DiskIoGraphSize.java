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
		gd.setHostTree(new Object[] {
				GraphDesc.HOST, GraphDesc.DISK, GraphDesc.DISKACTIVITY, GraphDesc.INDEX, GraphDesc.TITLE});
		gd.setViewTree(new Object[] {
						GraphDesc.DISK, GraphDesc.DISKACTIVITY, GraphDesc.HOST, "Average request size", GraphDesc.TITLE});
	}

	/**
	 * @param theStore
	 */
	public DiskIoGraphSize(Probe theStore) {
		super(theStore, gd);
		setGraphName("bsz." + probe.getName());
		setGraphTitle("E/S disque "+ ((IndexedProbe)probe).getIndexName() + "(taille des requetes)");
	}
}
