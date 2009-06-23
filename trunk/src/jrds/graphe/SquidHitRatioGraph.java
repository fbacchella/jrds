/*
 * Created on 25 janv. 2005
 *
 * TODO 
 */
package jrds.graphe;

import jrds.GraphDesc;
import jrds.Probe;
import jrds.GraphNode;

/**
 * @author bacchell
 *
 * TODO 
 */
public class SquidHitRatioGraph extends GraphNode {
	
	static final GraphDesc ds = new GraphDesc(9);
	static {
		ds.add("HttpRqt");
		ds.add("HttpHits");
		ds.add("HttpHitsRatio","HttpHits, HttpRqt, /, 100, *", GraphDesc.LINE, "HTTP hit ratio");

		ds.add("FqdnRequests");
		ds.add("FqdnHits");
		ds.add("FqdnHitsRatio","FqdnHits, FqdnRequests, /, 100, *", GraphDesc.LINE, "Name to IP hit ratio");

		ds.add("IpRequests");
		ds.add("IpHits");
		ds.add("IpHitsRatio","IpHits, IpRequests, /, 100, *", GraphDesc.LINE, "IP to name hit ratio");
		
		ds.setGraphName("squidhitratio");
		ds.setGraphTitle("Squid hit ratio on {1}");
		ds.setUpperLimit(100);
		ds.setVerticalLabel("%");
		ds.setHostTree(new Object[] {GraphDesc.HOST, GraphDesc.SERVICES, "Squid", "Squid hit ratio"} );
		ds.setViewTree(new Object[] {GraphDesc.SERVICES, "Squid", GraphDesc.HOST, "Squid hit ratio"});
	}
	
	/**
	 * @param theStore
	 */
	public SquidHitRatioGraph(Probe theStore) {
		super(theStore, ds);
	}
}
