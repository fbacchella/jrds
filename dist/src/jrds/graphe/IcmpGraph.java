/*
 * Created on 8 d�c. 2004
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
public class IcmpGraph extends RdsGraph {
	static final GraphDesc gd = new GraphDesc(1);
	static {
		gd.add("InMsgs", GraphDesc.LINE);
		gd.add("InErrors", GraphDesc.LINE);
		gd.add("OutMsgs", GraphDesc.LINE);
		gd.add("OutErrors", GraphDesc.LINE);

		gd.setFilename("icmp");
		gd.setGraphTitle("ICMP Info");
		gd.setVerticalLabel("Paquets/s");
		gd.setHostTree(GraphDesc.HNT);
		gd.setViewTree(GraphDesc.NTH);
	}

	/**
	 * @param theStore
	 */
	public IcmpGraph(Probe theStore) {
		super(theStore, gd);
	}
}
