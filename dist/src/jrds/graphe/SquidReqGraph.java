/*
 * Created on 25 janv. 2005
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
public class SquidReqGraph extends RdsGraph {
	
	static final GraphDesc ds = new GraphDesc(3);
	static {
		ds.add("HttpRqt", GraphDesc.LINE, "HTTP client requests");
		ds.add("ServerRequests", GraphDesc.LINE, "HTTP servers requests");
		ds.add("FqdnRequests", GraphDesc.LINE, "Direct name resolution ");
		ds.add("IpRequests", GraphDesc.LINE, "Reverse name resolution");

		ds.setFilename("squidreq");
		ds.setGraphTitle("Squid Requests");
		ds.setVerticalLabel("requests/s");
		ds.setHostTree(new Object[] {GraphDesc.HOST, GraphDesc.SERVICES, "Squid", GraphDesc.TITLE} );
		ds.setViewTree(new Object[] {GraphDesc.SERVICES, "Squid", GraphDesc.HOST, GraphDesc.TITLE});
	}
	
	/**
	 * @param theStore
	 */
	public SquidReqGraph(Probe theStore) {
		super(theStore, ds);
	}
}
