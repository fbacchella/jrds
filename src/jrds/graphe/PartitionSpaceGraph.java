/*
 * Created on 27 déc. 2004
 *
 * TODO 
 */
package jrds.graphe;

import java.awt.Color;

import jrds.GraphDesc;
import jrds.Probe;
import jrds.RdsGraph;
import jrds.probe.IndexedProbe;


/**
 * @author bacchell
 *
 * TODO 
 */
public class PartitionSpaceGraph extends RdsGraph {
	static final private GraphDesc gd= new GraphDesc(2);
	static {
		gd.add("Total");
		gd.add("Used", GraphDesc.AREA, Color.BLUE, "Used Space");
		gd.add("Available", "Total,Used,-", GraphDesc.STACK, Color.GREEN, "Available space");
		gd.setHostTree(new Object[] {GraphDesc.HOST, GraphDesc.DISK, "Systèmes de fichiers", GraphDesc.TITLE});
		gd.setViewTree(new Object[] {GraphDesc.DISK, "Systèmes de fichiers", GraphDesc.HOST, GraphDesc.INDEX});
		
	}
	/**
	 * @param theStore
	 */
	public PartitionSpaceGraph(Probe theStore) {
		super(theStore, gd);
		this.setGraphTitle("Occupation of file system " + ((IndexedProbe)probe).getIndexName());
		this.setGraphName(initFileNamePrefix());
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsGraph#initFileNamePrefix()
	 */
	protected String initFileNamePrefix() {
		String retval =  "fs-" + ((IndexedProbe)probe).getIndexName();
		retval = retval.replace('\\', '_');
		retval = retval.replace(':', '_');
		retval = retval.replace('/', '_');
		return retval;
	}
}
