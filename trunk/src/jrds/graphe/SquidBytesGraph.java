/*
 * Created on 25 janv. 2005
 *
 * TODO 
 */
package jrds.graphe;

import jrds.GraphDesc;
import jrds.Probe;
import jrds.GraphNode;

/**
 * @author bacchell
 *
 * TODO 
 */
public class SquidBytesGraph extends GraphNode {
	
	static final GraphDesc ds = new GraphDesc(2);
	static {
		ds.add("HttpOutKb");
		ds.add("HttpOut", "HttpOutKb, 8192, *", GraphDesc.LINE, "Bits send to client");
		ds.add("HttpInKb");
		ds.add("HttpIn", "HttpInKb, 8192, *", GraphDesc.LINE, "Bits received from client");
		ds.add("ServerOutKb");
		ds.add("ServerOut", "ServerOutKb, 8192, *", GraphDesc.LINE, "Bits send to servers");
		ds.add("ServerInKb");
		ds.add("ServerIn", "ServerInKb, 8192, *", GraphDesc.LINE, "Bits received from servers");


		ds.setGraphName("squidbytes");
		ds.setGraphTitle("Squid bytes transfered on {1}");
		ds.setVerticalLabel("Bit/s");
		ds.setHostTree(new Object[] {GraphDesc.HOST, GraphDesc.SERVICES, "Squid", "Squid bytes transfered"} );
		ds.setViewTree(new Object[] {GraphDesc.SERVICES, "Squid", GraphDesc.HOST, "Squid bytes transfered"});
	}
	
	/**
	 * @param theStore
	 */
	public SquidBytesGraph(Probe theStore) {
		super(theStore, ds);
	}
}
