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
public class DiskIoGraphBytes extends RdsGraph {
	private static final GraphDesc gd = new GraphDesc(2);
	static {
		gd.add("diskIONRead", GraphDesc.LINE, Color.GREEN,"Number of bytes read");
		gd.add("diskIONWritten", GraphDesc.LINE, Color.BLUE,"Number of bytes written");
		gd.setVerticalLabel("Bytes/s");
		gd.setHostTree(new Object[] {
				GraphDesc.HOST, GraphDesc.DISK, GraphDesc.DISKACTIVITY, GraphDesc.INDEX, GraphDesc.TITLE});
		gd.setViewTree(new Object[] {
						GraphDesc.DISK, GraphDesc.DISKACTIVITY, GraphDesc.HOST, "Activity as bytes/s", GraphDesc.TITLE});
		gd.setGraphName("bytes-{2}");
		gd.setGraphTitle("E/S (octets) disque on {2} on {1}");
	}

	/**
	 * @param theStore
	 */
	public DiskIoGraphBytes(Probe theStore) {
		super(theStore, gd);
		//setGraphName("BYTES." + probe.getName());
		//setGraphTitle("E/S disque " + ((IndexedProbe)probe).getIndexName() +"(octets)");
	}
}
