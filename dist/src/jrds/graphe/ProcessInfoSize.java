/*
 * Created on 21 d�c. 2004
 *
 * TODO 
 */
package jrds.graphe;

import jrds.GraphDesc;
import jrds.Probe;
import jrds.RdsGraph;
import jrds.probe.IndexedProbe;


/**
 * @author bacchell
 *
 * TODO 
 */
public class ProcessInfoSize extends RdsGraph {
	static final private String MIN = "Minimum";
	static final private String MAX = "Maximum";
	static final private String AVERAGE = "Average";
	static final private String NUM = "Number";
	static final private GraphDesc gd = new GraphDesc(3);
	static {
		gd.add(MIN, GraphDesc.LINE, MIN);
		gd.add(MAX, GraphDesc.LINE, MAX);
		gd.add(AVERAGE, GraphDesc.LINE, AVERAGE);
		gd.setHostTree(new Object[] {GraphDesc.HOST, GraphDesc.SYSTEM, "Processus", GraphDesc.INDEX, GraphDesc.TITLE});
		gd.setViewTree(new Object [] {GraphDesc.SYSTEM, "Processus", GraphDesc.HOST, GraphDesc.INDEX, GraphDesc.TITLE});
	}

	/**
	 * @param theStore
	 */
	public ProcessInfoSize(Probe theStore) {
		super(theStore, gd);
		this.setFilename("size." + ((IndexedProbe)probe).getIndexName());
		this.setGraphTitle("Memory for Process " + ((IndexedProbe)probe).getIndexName());
	}
}
