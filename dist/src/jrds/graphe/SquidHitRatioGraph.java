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
public class SquidHitRatioGraph extends RdsGraph {
	
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
		
		ds.setFilename("squidhitratio");
		ds.setGraphTitle("Squid hit ratio");
		ds.setUpperLimit(100);
		ds.setVerticalLabel("%");
		ds.setHostTree(new Object[] {GraphDesc.HOST, GraphDesc.SERVICES, "Squid", GraphDesc.TITLE} );
		ds.setViewTree(new Object[] {GraphDesc.SERVICES, "Squid", GraphDesc.HOST, GraphDesc.TITLE});
	}
	
	/**
	 * @param theStore
	 */
	public SquidHitRatioGraph(Probe theStore) {
		super(theStore, ds);
	}
}
