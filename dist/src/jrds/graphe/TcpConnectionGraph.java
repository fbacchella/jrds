/*
 * Created on 8 déc. 2004
 *
 * TODO 
 */
package jrds.graphe;

import java.awt.Color;
import java.util.LinkedList;

import jrds.GraphDesc;
import jrds.Probe;
import jrds.RdsGraph;


/**
 * @author bacchell
 *
 * TODO 
 */
public class TcpConnectionGraph extends RdsGraph {
	static final GraphDesc gd = new GraphDesc(4);
	static {
		gd.add("ActiveOpens", GraphDesc.LINE, Color.BLUE, "Active Opens");
		gd.add("PassiveOpens", GraphDesc.LINE, Color.CYAN, "Passive Opens");
		gd.add("AttemptFails", GraphDesc.LINE, Color.RED, "Connections attempt failed");
		gd.add("EstabResets", GraphDesc.LINE, Color.BLACK, "Connections resets");

		gd.setGraphTitle("Connections TCP");
		gd.setFilename("tcp");
		gd.setVerticalLabel("Requests/s");
	}

	/**
	 * @param theStore
	 */
	public TcpConnectionGraph(Probe theStore) {
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
