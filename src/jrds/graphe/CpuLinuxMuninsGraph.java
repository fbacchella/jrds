/*
 * Created on 11 janv. 2005
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
public class CpuLinuxMuninsGraph extends RdsGraph {
	static final GraphDesc gd = new GraphDesc(7);
	static {
		gd.add("system", GraphDesc.AREA, "system");
		gd.add("user", GraphDesc.STACK, "user");
		gd.add("nice", GraphDesc.STACK, "nice");
		gd.add("idle", GraphDesc.STACK, "idle");
		gd.add("iowait", GraphDesc.STACK, "iowait");
		gd.add("irq", GraphDesc.STACK, "irq");
		gd.add("softirq", GraphDesc.STACK, "softirq");
		gd.setFilename("cpulinux");
		gd.setGraphTitle("CPU usage");
		gd.setVerticalLabel("%");
		gd.setHostTree(GraphDesc.HSLT);
		gd.setViewTree(GraphDesc.SLHT);
	}

	/**
	 * @param theStore
	 */
	public CpuLinuxMuninsGraph(Probe theStore) {
		super(theStore, gd);
	}
}
