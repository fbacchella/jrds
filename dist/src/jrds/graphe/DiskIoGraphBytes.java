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
public class DiskIoGraphBytes extends RdsGraph {
	private static final GraphDesc gd = new GraphDesc(2);
	static {
		gd.add("diskIONRead", GraphDesc.LINE, Color.GREEN,"Number of bytes read");
		gd.add("diskIONWritten", GraphDesc.LINE, Color.BLUE,"Number of bytes written");
		gd.setVerticalLabel("Bytes/s");
		gd.setHostTree(GraphDesc.HDAIT);
		gd.setViewTree(GraphDesc.DAHIT);
		gd.setSubTitle("Activity as bytes/s");
	}

	/**
	 * @param theStore
	 */
	public DiskIoGraphBytes(Probe theStore) {
		super(theStore, gd);
		setFilename("BYTES." + probe.getName());
		setGraphTitle("E/S disque " + ((IndexedProbe)probe).getIndexName() +"(octets)");
	}
}
