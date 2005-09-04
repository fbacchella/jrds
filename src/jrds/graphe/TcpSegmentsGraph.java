/*
 * Created on 8 déc. 2004
 *
 * TODO 
 */
package jrds.graphe;

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

		gd.setGraphName("Segments TCP échangés");
		gd.setGraphName("tcpseg");
		gd.setVerticalLabel("Segments/s");
		gd.setHostTree(new Object[] { GraphDesc.HOST, GraphDesc.NETWORK, GraphDesc.TCP, GraphDesc.TITLE});
		gd.setViewTree(new Object[] { GraphDesc.NETWORK,  GraphDesc.TCP, GraphDesc.TITLE, GraphDesc.HOST});
	}

	/**
	 * @param theStore
	 */
	public TcpSegmentsGraph(Probe theStore) {
		super(theStore, gd);
	}
}
