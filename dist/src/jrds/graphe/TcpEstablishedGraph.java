/*
 * Created on 8 déc. 2004
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
public class TcpEstablishedGraph extends RdsGraph {
	static final GraphDesc gd = new GraphDesc(1);
	static {
		gd.add("CurrEstab", GraphDesc.AREA,"ESTABLISHED or CLOSE-WAIT connections");

		gd.setGraphTitle("Connections TCP établies");
		gd.setFilename("tcpest");
		gd.setVerticalLabel("Number");
	}

	/**
	 * @param theStore
	 */
	public TcpEstablishedGraph(Probe theStore) {
		super(theStore, gd);
	}
	
	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsGraph#getTreePathByHost()
	 */
	public LinkedList getTreePathByHost() {
		LinkedList retValue = new LinkedList();
		retValue.add(this.probe.getHost().getName());
		retValue.add("Réseau");
		retValue.add("TCP");
		retValue.add(getGraphTitle());
		return retValue;
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsGraph#getTreePathByView()
	 */
	public LinkedList getTreePathByView() {
		LinkedList retValue = new LinkedList();
		retValue.add("Réseau");
		retValue.add("TCP");
		retValue.add(probe.getHost().getName());
		retValue.add(getGraphTitle());

		return retValue;
	}
}
