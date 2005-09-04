/*
 * Created on 8 déc. 2004
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
public class TcpEstablishedGraph extends RdsGraph {
	static final GraphDesc gd = new GraphDesc(1);
	static {
		gd.add("CurrEstab", GraphDesc.AREA,"ESTABLISHED or CLOSE-WAIT connections");

		gd.setGraphName("Connections TCP établies");
		gd.setGraphName("tcpest");
		gd.setVerticalLabel("Number");
		gd.setHostTree(new Object[] { GraphDesc.HOST, GraphDesc.NETWORK, GraphDesc.TCP, GraphDesc.TITLE});
		gd.setViewTree(new Object[] { GraphDesc.NETWORK,  GraphDesc.TCP, GraphDesc.TITLE, GraphDesc.HOST});
	}

	/**
	 * @param theStore
	 */
	public TcpEstablishedGraph(Probe theStore) {
		super(theStore, gd);
	}
}
