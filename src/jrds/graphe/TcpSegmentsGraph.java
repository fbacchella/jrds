/*
 * Created on 8 déc. 2004
 *
 * TODO 
 */
package jrds.graphe;

import java.util.LinkedList;

import jrds.GraphDesc;
import jrds.Probe;
import jrds.RdsGraph;


/**
 * @author bacchell
 *
 * TODO 
 */
public class TcpSegmentsGraph extends RdsGraph {
	static final GraphDesc gd = new GraphDesc(3);
	static {
		gd.add("InSegs", GraphDesc.LINE, "Number of segments received");
		gd.add("OutSegs", GraphDesc.LINE, "Number of segments sent");
		gd.add("RetransSegs", GraphDesc.LINE, "Number of segments retransmitted");

		gd.setGraphTitle("Segments TCP échangés");
		gd.setFilename("tcpseg");
		gd.setVerticalLabel("Segments/s");
	}

	/**
	 * @param theStore
	 */
	public TcpSegmentsGraph(Probe theStore) {
		super(theStore, gd);
	}
	
	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsGraph#getTreePathByHost()
	 */
	public LinkedList getTreePathByHost() {
		LinkedList retValue = new LinkedList();
		retValue.add(this.probe.getHost().getName());
		retValue.add("Réseau");
		retValue.add("TCP");
		retValue.add(getGraphTitle());
		return retValue;
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsGraph#getTreePathByView()
	 */
	public LinkedList getTreePathByView() {
		LinkedList retValue = new LinkedList();
		retValue.add("Réseau");
		retValue.add("TCP");
		retValue.add(probe.getHost().getName());
		retValue.add(getGraphTitle());

		return retValue;
	}
}
