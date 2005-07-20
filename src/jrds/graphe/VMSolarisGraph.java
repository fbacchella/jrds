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
public class VMSolarisGraph extends RdsGraph {
	
	static final GraphDesc ds = new GraphDesc(4);
	static {
		ds.add("rsVPagesIn", GraphDesc.LINE, "Number of pages read in from disk");
		ds.add("rsVPagesOut", GraphDesc.LINE, "Number of pages written to disk");
		ds.add("rsVSwapIn", GraphDesc.LINE, "Number of pages swapped in");
		ds.add("rsVSwapOut", GraphDesc.LINE, "Number of pages swapped out");
		
		ds.setFilename("vmsolaris");
		ds.setGraphTitle("VM activity");
		ds.setVerticalLabel("Operation/s");
	}
	
	/**
	 * @param theStore
	 */
	public VMSolarisGraph(Probe theStore) {
		super(theStore, ds);
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsGraph#getTreePathByHost()
	 */
	public LinkedList getTreePathByHost() {
		LinkedList retValue = new LinkedList();
		retValue.add(this.probe.getHost().getName());
		retValue.add("System");
		retValue.add("Memory");
		retValue.add(getGraphTitle());
		return retValue;
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsGraph#getTreePathByView()
	 */
	public LinkedList getTreePathByView() {
		LinkedList retValue = new LinkedList();
		retValue.add("System");
		retValue.add("Memory");
		retValue.add(getGraphTitle());
		retValue.add(this.probe.getHost().getName());
		return retValue;
	}

}
