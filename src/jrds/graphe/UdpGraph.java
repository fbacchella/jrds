/*
 * Created on 8 dŽc. 2004
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
public class UdpGraph extends RdsGraph {
	static final private GraphDesc gd = new GraphDesc(4);
	static {
		gd.add("InDatagrams", GraphDesc.LINE, "In Datagrams");
		gd.add("NoPorts", GraphDesc.LINE, "No Ports");
		gd.add("InErrors", GraphDesc.LINE, "In Errors");
		gd.add("OutDatagrams", GraphDesc.LINE, "Out Datagrams");
		gd.setFilename("udp");
		gd.setGraphName("Activité UDP");
		gd.setHostTree(GraphDesc.HNT);
		gd.setViewTree(GraphDesc.NTH);
	}

	/**
	 * @param theStore
	 */
	public UdpGraph(Probe theStore) {
		super(theStore, gd);
	}
	
}
