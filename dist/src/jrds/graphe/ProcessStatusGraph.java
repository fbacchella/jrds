/*
 * Created on 3 févr. 2005
 *
 * TODO 
 */
package jrds.graphe;

import java.util.LinkedList;

import jrds.GraphDesc;
import jrds.Probe;
import jrds.RdsGraph;


/**
 * @author bacchell
 *
 * TODO 
 */
public class ProcessStatusGraph extends RdsGraph {
	
	static final private String RUNNABLE="R";
	static final private String STOPPED="T";
	static final private String INPAGEWAIT="P";
	static final private String NONINTERRUPTABLEWAIT="D";
	static final private String SLEEPING="S";
	static final private String IDLE="I";
	static final private String ZOMBIE="Z";

	static final GraphDesc ds = new GraphDesc(7);
	static {
		ds.add(SLEEPING, GraphDesc.AREA, GraphDesc.COLOR1, "Sleeping (less than 20 seconds)");
		ds.add(IDLE, GraphDesc.STACK, GraphDesc.COLOR6, "Idle (more than 20 seconds)");
		ds.add(RUNNABLE, GraphDesc.STACK, GraphDesc.COLOR2, "Runnable");
		ds.add(STOPPED, GraphDesc.STACK, GraphDesc.COLOR3, "Stopped");
		ds.add(INPAGEWAIT, GraphDesc.STACK, GraphDesc.COLOR4, "In page wait");
		ds.add(NONINTERRUPTABLEWAIT, GraphDesc.STACK, GraphDesc.COLOR5, "Non-interruptable wait");
		ds.add(ZOMBIE, GraphDesc.STACK, GraphDesc.COLOR7, "Zombie");
		ds.setFilename("pslist");
		ds.setGraphTitle("Number of process");
	}

	/**
	 * @param theStore
	 */
	public ProcessStatusGraph(Probe theStore) {
		super(theStore, ds);
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsGraph#getTreePathByHost()
	 */
	public LinkedList getTreePathByHost() {
		LinkedList retValue = new LinkedList();
		retValue.add(this.probe.getHost().getName());
		retValue.add("Système");
		retValue.add("Charge");
		retValue.add(getGraphTitle());
		return retValue;
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsGraph#getTreePathByView()
	 */
	public LinkedList getTreePathByView() {
		LinkedList retValue = new LinkedList();
		retValue.add("Système");
		retValue.add("Charge");
		retValue.add(getGraphTitle());
		retValue.add(probe.getHost().getName());
		return retValue;
	}
}
