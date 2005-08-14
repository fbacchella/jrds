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
public class IpErrorGraph extends RdsGraph {
	
	static final GraphDesc ds = new GraphDesc(7);
	static {
		ds.add("ipInHdrErrors", GraphDesc.LINE);
		ds.add("ipInAddrErrors", GraphDesc.LINE);
		ds.add("ipInUnknownProtos", GraphDesc.LINE);
		ds.add("ipInDiscards", GraphDesc.LINE);
		ds.add("ipReasmFails", GraphDesc.LINE);
		ds.add("ipFragFails", GraphDesc.LINE);
		ds.add("ipRoutingDiscards", GraphDesc.LINE);
		
		ds.setFilename("iperrors");
		ds.setVerticalLabel("paquets/s");
		ds.setGraphName("IP errors activity");
		
		ds.setHostTree(new Object[] { GraphDesc.HOST, GraphDesc.NETWORK, GraphDesc.IP, GraphDesc.TITLE});
		ds.setViewTree(new Object[] { GraphDesc.NETWORK, GraphDesc.IP, GraphDesc.HOST, GraphDesc.TITLE});
	}

	/**
	 * @param theStore
	 */
	public IpErrorGraph(Probe theStore) {
		super(theStore, ds);
	}
}
