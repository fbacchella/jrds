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
public class SquidCpu extends GraphNode {
	
	static final GraphDesc ds = new GraphDesc(1);
	static {
		ds.add("CpuUsage", GraphDesc.LINE, "CPU usage");

		ds.setGraphName("squidcpu");
		ds.setGraphTitle("Squid CPU usage on {1}");
		ds.setVerticalLabel("%");
		ds.setHostTree(new Object[] {GraphDesc.HOST, GraphDesc.SERVICES, "Squid", "Squid CPU usage"} );
		ds.setViewTree(new Object[] {GraphDesc.SERVICES, "Squid", GraphDesc.HOST, "Squid CPU usage"});
	}
	
	/**
	 * @param theStore
	 */
	public SquidCpu(Probe theStore) {
		super(theStore, ds);
	}
}
