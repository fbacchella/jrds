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
public class IfPacketSize extends RdsGraph {
	static final GraphDesc ds = new GraphDesc(6);
	static {
		ds.add("ifOutUcastPkts");
		ds.add("ifOutOctets");
		ds.add("outPktSize","ifOutOctets, ifOutUcastPkts,/", GraphDesc.AREA, Color.GREEN,"average packets size send") ;

		ds.add("ifInUcastPkts");
		ds.add("ifInOctets");
		ds.add("inPktSize","0, ifInOctets, ifInUcastPkts,/,-", GraphDesc.AREA, Color.BLUE,"average packets size received") ;
	
		ds.setLowerLimit(Double.NaN);
		ds.setVerticalLabel("bytes");
}


	/**
	 * @param theStore
	 */
	public IfPacketSize(Probe theStore) {
		super(theStore, ds);
		setFilename("ifpktssz-" + ((IndexedProbe)probe).getIndexName());
		setGraphTitle("Interface " + ((IndexedProbe)probe).getIndexName() + " packets size");
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
