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
public class IpGraph extends RdsGraph {
	
	static final GraphDesc ds = new GraphDesc(13);
	static {
		ds.add("ipInReceives", GraphDesc.LINE);
		ds.add("ipForwDatagrams", GraphDesc.LINE);
		ds.add("ipInDelivers", GraphDesc.LINE);
		ds.add("ipOutRequests", GraphDesc.LINE);
		ds.add("ipReasmReqds", GraphDesc.LINE);
		ds.add("ipReasmOKs", GraphDesc.LINE);
		ds.add("ipFragOKs", GraphDesc.LINE);
		ds.add("ipFragCreates", GraphDesc.LINE);
		
		ds.setFilename("ip");
		ds.setGraphTitle("IP activity");
		ds.setVerticalLabel("paquets/s");
		ds.setHostTree(new Object[] { GraphDesc.HOST, GraphDesc.NETWORK, GraphDesc.IP, GraphDesc.TITLE});
		ds.setViewTree(new Object[] { GraphDesc.NETWORK, GraphDesc.IP, GraphDesc.HOST, GraphDesc.TITLE});
	}

	/**
	 * @param theStore
	 */
	public IpGraph(Probe theStore) {
		super(theStore, ds);
	}
}
