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
public class SquidCpu extends RdsGraph {
	
	static final GraphDesc ds = new GraphDesc(1);
	static {
		ds.add("CpuUsage", GraphDesc.LINE, "CPU usage");

		ds.setFilename("squidcpu");
		ds.setGraphTitle("Squid CPU usage");
		ds.setVerticalLabel("%");
		ds.setHostTree(new Object[] {GraphDesc.HOST, GraphDesc.SERVICES, "Squid", GraphDesc.TITLE} );
		ds.setViewTree(new Object[] {GraphDesc.SERVICES, "Squid", GraphDesc.HOST, GraphDesc.TITLE});
	}
	
	/**
	 * @param theStore
	 */
	public SquidCpu(Probe theStore) {
		super(theStore, ds);
	}
}
