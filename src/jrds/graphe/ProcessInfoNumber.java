/*
 * Created on 21 déc. 2004
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
public class ProcessInfoNumber extends RdsGraph {
	static final private String MIN = "Minimum";
	static final private String MAX = "Maximum";
	static final private String AVERAGE = "Average";
	static final private String NUM = "Number";
	
	static final private GraphDesc gd = new GraphDesc(1);
	static {
		gd.add(NUM, GraphDesc.LINE);
		gd.setHostTree(new Object[] {GraphDesc.HOST, GraphDesc.SYSTEM, "Processus", GraphDesc.INDEX, "Number of Process"});
		gd.setViewTree(new Object [] {GraphDesc.SYSTEM, "Processus", GraphDesc.HOST, GraphDesc.INDEX, "Number of Process"});
		gd.setGraphName("num.{2}");
		gd.setGraphTitle("Number of Process {2} on {1}");
		
	}
	
	/**
	 * @param theStore
	 */
	public ProcessInfoNumber(Probe theStore) {
		super(theStore, gd);
	}
}
