/*
 * Created on 7 févr. 2005
 *
 * TODO 
 */
package jrds.graphe;

import java.awt.Color;
import java.util.LinkedList;

import jrds.GraphDesc;
import jrds.Probe;
import jrds.RdsGraph;
import jrds.probe.IndexedProbe;


/**
 * @author bacchell
 *
 * TODO 
 */
public class IfPacketsGraph extends RdsGraph {
	static final GraphDesc ds = new GraphDesc(6);
	static {
		ds.add(GraphDesc.COMMENT,"Upward graph");
		ds.add("ifOutUcastPkts", GraphDesc.AREA, Color.GREEN,"packets sends/s");
		ds.add("ifOutErrors", GraphDesc.LINE, Color.RED,"packets in error send/s");

		ds.add("ifInUcastPkts");
		ds.add("ifInErrors");
		ds.add(GraphDesc.COMMENT,"Downward graph");
		ds.add("ifInUcastInversed","0, ifInUcastPkts,-", GraphDesc.AREA, Color.BLUE,"packets received/s") ;
		ds.add("ifInErrorsInversed","0, ifInErrors,-", GraphDesc.LINE, Color.RED,"packets in error received/s");

		ds.setLowerLimit(Double.NaN);
		ds.setVerticalLabel("paquets/s");
	}

	/**
	 * @param theStore
	 */
	public IfPacketsGraph(Probe theStore) {
		super(theStore, ds);
		setFilename("ifpkts-" + ((IndexedProbe)probe).getIndexName());
		setGraphTitle("Interface " + ((IndexedProbe)probe).getIndexName() + " packets");
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsGraph#getTreePathByHost()
	 */
	public LinkedList getTreePathByHost() {
		LinkedList retValue = new LinkedList();
		retValue.add(this.probe.getHost().getName());
		retValue.add("Réseau");
		retValue.add("Interfaces");
		retValue.add(((IndexedProbe)probe).getIndexName());
		retValue.add(getGraphTitle());
		return retValue;
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsGraph#getTreePathByView()
	 */
	public LinkedList getTreePathByView() {
		LinkedList retValue = new LinkedList();
		retValue.add("Réseau");
		retValue.add("Interfaces");
		retValue.add(this.probe.getHost().getName());
		retValue.add(((IndexedProbe)probe).getIndexName());
		retValue.add(getGraphTitle());
		return retValue;
	}
}
